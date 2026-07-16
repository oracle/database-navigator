/*
 * Copyright 2024-2025 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.ml.backend.dbms;

import com.dbn.common.Priority;
import com.dbn.common.cloud.CloudSourceConfig;
import com.dbn.common.util.Naming;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMachineLearningInterface;
import com.dbn.ml.backend.model.MLModelMetadata;
import com.dbn.ml.backend.model.MLTrainingContext;
import com.dbn.ml.model.MLTaskType;
import com.dbn.ml.model.source.MLSourceNames;
import com.dbn.ml.model.source.MLSourceType;
import com.dbn.ml.model.trainer.MLTrainerConfig;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.dbn.nls.NlsResources.txt;

/**
 * Oracle DBMS_DATA_MINING backend implementation.
 * Trains models directly in the database using Oracle's in-database ML capabilities.
 *
 * Follows Oracle's recommended workflow:
 * 1. Split data into training/test sets using SAMPLE SEED
 * 2. Train model on training set only
 * 3. Evaluate using COMPUTE_CONFUSION_MATRIX on test set
 * 4. For binary classification, also compute ROC/AUC
 *
 * @author Oracle
 */
@Slf4j
public class DBMSBackend {

    private final ConnectionHandler connection;
    private final DBMSDataManager dataManager;
    private final DBMSSettingsBuilder settingsBuilder;

    public DBMSBackend(ConnectionHandler connection) {
        this.connection = connection;
        this.dataManager = new DBMSDataManager();
        this.settingsBuilder = new DBMSSettingsBuilder();
    }

    /**
     * Prepares training data and submits CREATE_MODEL as a DBMS_SCHEDULER job.
     * Returns immediately — Oracle trains the model server-side.
     *
     * @return the model name that will be created by Oracle
     */
    public String submitAsync(MLTrainingContext context) throws Exception {
        log.info("Submitting async training job for task: {}", context.getTaskType());
        context.setTrainingStartTime(System.currentTimeMillis());

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        // Data prep — fast, requires connection
        String sourceTableName = prepareDataSource(context);
        String trainTableName = "ML_TRAIN_" + timestamp;
        String testTableName = "ML_TEST_" + timestamp;
        splitData(context, sourceTableName, trainTableName, testTableName);
        String settingsTableName = createSettingsTable(context, timestamp);
        String modelName = generateModelName(context, timestamp);

        String miningFunction = DBMSAlgorithmType.getMiningFunction(context.getTaskType());
        String targetColumn = context.getFeatureConfig().getLabelColumns().get(0);

        // Pre-training validation
        DBMSAlgorithmType algorithmType = DBMSAlgorithmType.fromTrainerType(context.getTrainerType());
        if (context.getTaskType() == MLTaskType.CLASSIFICATION && algorithmType == DBMSAlgorithmType.LOGISTIC_REGRESSION) {
            int classCount = getDistinctClassCount(trainTableName, targetColumn);
            if (classCount > 2) {
                throw new IllegalArgumentException(txt("msg.machineLearning.exception.LogisticRegressionBinaryOnly", targetColumn, classCount));
            }
        }

        // Build scheduler job name and PL/SQL action
        String jobName = "ML_JOB_" + timestamp;
        String jobAction = "BEGIN DBMS_DATA_MINING.CREATE_MODEL(" +
                "model_name=>'" + modelName + "'," +
                "mining_function=>DBMS_DATA_MINING." + miningFunction + "," +
                "data_table_name=>'" + trainTableName + "'," +
                "case_id_column_name=>'CASE_ID'," +
                "target_column_name=>'" + targetColumn + "'," +
                "settings_table_name=>'" + settingsTableName + "'" +
                "); END;";

        // Submit the scheduler job — returns immediately
        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                txt("prc.machineLearning.title.SubmittingTrainingJob"),
                txt("prc.machineLearning.text.SubmittingTrainingJob", modelName),
                getProject(),
                connection.getConnectionId(),
                conn -> connection.getInterfaces().getMachineLearningInterface().submitTrainingJob(conn, jobName, jobAction));

        context.setSchedulerJobName(jobName);
        context.setModelName(modelName);

        log.info("Training job {} submitted for model: {}", jobName, modelName);
        return modelName;
    }

    public String getSchedulerJobState(String jobName) throws SQLException {
        return DatabaseInterfaceInvoker.load(Priority.LOW,
                getProject(),
                connection.getConnectionId(),
                conn -> connection.getInterfaces().getMachineLearningInterface().getSchedulerJobState(conn, jobName));
    }

    public String getSchedulerJobRunStatus(String jobName) throws SQLException {
        return DatabaseInterfaceInvoker.load(Priority.LOW,
                getProject(),
                connection.getConnectionId(),
                conn -> connection.getInterfaces().getMachineLearningInterface().getSchedulerJobRunStatus(conn, jobName));
    }

    public void dropSchedulerJob(String jobName) throws SQLException {
        DatabaseInterfaceInvoker.execute(Priority.LOW,
                getProject(),
                connection.getConnectionId(),
                conn -> connection.getInterfaces().getMachineLearningInterface().dropSchedulerJob(conn, jobName));
    }

    public DBMSModelHandle loadModelHandle(MLTrainingContext context, String modelName) throws SQLException {
        String trainTableName = context.getTrainTableName();
        String testTableName = context.getTestTableName();
        String settingsTableName = context.getSettingsTableName();

        if (trainTableName == null || testTableName == null || settingsTableName == null) {
            throw new IllegalStateException("Training context is missing async table references");
        }

        Integer classCount = null;
        Integer outputDimensions = null;
        List<String> classValues = null;

        if (context.getTaskType() == MLTaskType.CLASSIFICATION) {
            String targetColumn = context.getFeatureConfig().getLabelColumns().get(0);
            classValues = getClassValues(trainTableName, targetColumn);
            classCount = classValues != null ? classValues.size() : 0;
        } else {
            outputDimensions = 1;
        }

        MLModelMetadata metadata = MLModelMetadata.builder()
                .featureNames(context.getFeatureConfig().getFeatureColumns())
                .labelNames(context.getFeatureConfig().getLabelColumns())
                .algorithmName(context.getAlgorithmName())
                .classCount(classCount)
                .outputDimensions(outputDimensions)
                .build();

        DBMSModelHandle modelHandle = new DBMSModelHandle(
                modelName,
                connection,
                context.getTaskType(),
                metadata,
                trainTableName,
                settingsTableName
        );

        modelHandle.setTestTableName(testTableName);
        modelHandle.setClassValues(classValues);
        return modelHandle;
    }

    public DBMSEvaluationResult evaluate(DBMSModelHandle modelHandle, MLTrainingContext context) throws Exception {
        log.info("Evaluating model: {} on test data", modelHandle.getModelName());

        return DatabaseInterfaceInvoker.load(Priority.HIGH,
                txt("prc.machineLearning.title.EvaluatingModel"),
                txt("prc.machineLearning.text.ComputingEvaluationMetrics"),
                getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();
                    if (context.getTaskType() == MLTaskType.CLASSIFICATION) {
                        return evaluateClassificationProper(mlInterface, conn, modelHandle, context);
                    } else {
                        return evaluateRegressionProper(mlInterface, conn, modelHandle, context);
                    }
                });
    }

    public void cleanup(MLTrainingContext context) throws Exception {
        log.info("Cleaning up DBMS resources");

        DatabaseInterfaceInvoker.execute(Priority.LOW,
                txt("prc.machineLearning.title.Cleanup"),
                txt("prc.machineLearning.text.Cleanup"),
                getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();

                    // Drop staging table if created from CSV
                    if (context.getStagingTableName() != null && context.isShouldCleanupStagingTable()) {
                        dropTableSafe(mlInterface, conn, context.getStagingTableName());
                    }

                    // Drop training table
                    if (context.getTrainTableName() != null) {
                        dropTableSafe(mlInterface, conn, context.getTrainTableName());
                    }

                    // Drop test table
                    if (context.getTestTableName() != null) {
                        dropTableSafe(mlInterface, conn, context.getTestTableName());
                    }

                    // Drop settings table
                    if (context.getSettingsTableName() != null) {
                        dropTableSafe(mlInterface, conn, context.getSettingsTableName());
                    }

                    // Drop evaluation tables
                    if (context.getApplyResultTableName() != null) {
                        dropTableSafe(mlInterface, conn, context.getApplyResultTableName());
                    }
                    if (context.getConfusionMatrixTableName() != null) {
                        dropTableSafe(mlInterface, conn, context.getConfusionMatrixTableName());
                        dropTableSafe(mlInterface, conn, context.getConfusionMatrixTableName() + "_ACC");
                    }
                    if (context.getRocTableName() != null) {
                        dropTableSafe(mlInterface, conn, context.getRocTableName());
                        dropTableSafe(mlInterface, conn, context.getRocTableName() + "_AUC");
                    }
                    if (context.getLiftTableName() != null) {
                        dropTableSafe(mlInterface, conn, context.getLiftTableName());
                    }
                });
    }

    // ==================== Data Splitting (Oracle Recommended) ====================

    /**
     * Splits data into training and test sets using Oracle's SAMPLE SEED.
     * This follows Oracle's recommended approach for ML model evaluation.
     */
    private void splitData(MLTrainingContext context, String sourceTableName,
                          String trainTableName, String testTableName) throws SQLException {

        MLTrainerConfig trainerConfig = context.getTrainerConfig();
        int trainPercent = (int) (trainerConfig.getTrainTestSplitRatio() * 100);
        long seed = trainerConfig.isUseFixedSeed() ? trainerConfig.getRandomSeed() : System.currentTimeMillis();

        log.info("Creating training table: {} ({}% of data, seed={})", trainTableName, trainPercent, seed);

        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                txt("prc.machineLearning.title.SplittingData"),
                txt("prc.machineLearning.text.CreatingTrainTestSplit"),
                getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();

                    // Create training table with SAMPLE SEED
                    mlInterface.createTrainingTable(conn, trainTableName, sourceTableName, trainPercent, seed);

                    // Create test table (source MINUS training)
                    log.info("Creating test table: {} (remaining data)", testTableName);
                    mlInterface.createTestTable(conn, testTableName, sourceTableName, trainTableName);

                    // Add CASE_ID columns for evaluation procedures
                    mlInterface.addCaseIdColumn(conn, trainTableName);
                    mlInterface.addCaseIdColumn(conn, testTableName);

                    // Get row counts
                    int trainCount = mlInterface.getRowCount(conn, trainTableName);
                    int testCount = mlInterface.getRowCount(conn, testTableName);

                    context.setTrainingDataSize(trainCount);
                    context.setTestingDataSize(testCount);

                    log.info("Data split complete: {} training rows, {} test rows", trainCount, testCount);
                });

        // Store table names in context for cleanup
        context.setTrainTableName(trainTableName);
        context.setTestTableName(testTableName);
    }

    // ==================== Proper Evaluation (Oracle DBMS_DATA_MINING procedures) ====================

    /**
     * Evaluates classification model using Oracle's COMPUTE_CONFUSION_MATRIX.
     * For binary classification, also computes ROC/AUC.
     */
    private DBMSEvaluationResult evaluateClassificationProper(
            DatabaseMachineLearningInterface mlInterface,
            DBNConnection conn,
            DBMSModelHandle modelHandle,
            MLTrainingContext context) throws SQLException {

        String modelName = modelHandle.getModelName();
        String testTableName = modelHandle.getTestTableName();
        String targetColumn = context.getFeatureConfig().getLabelColumns().get(0);
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        // Table names for evaluation artifacts
        String applyResultTable = "ML_APPLY_" + timestamp;
        String confusionMatrixTable = "ML_CM_" + timestamp;

        // Store for cleanup
        context.setApplyResultTableName(applyResultTable);
        context.setConfusionMatrixTableName(confusionMatrixTable);

        // Step 1: Create apply results table (predictions on test data)
        log.info("Creating apply results: {} from model {} on test table {}",
                applyResultTable, modelName, testTableName);
        mlInterface.createApplyResults(conn, applyResultTable, modelName, testTableName);

        // Step 2: Compute confusion matrix using Oracle's COMPUTE_CONFUSION_MATRIX
        log.info("Computing confusion matrix using DBMS_DATA_MINING.COMPUTE_CONFUSION_MATRIX");
        mlInterface.computeConfusionMatrix(conn, applyResultTable, testTableName, targetColumn, confusionMatrixTable);

        // Step 3: Get accuracy
        double accuracy = mlInterface.getAccuracy(conn, confusionMatrixTable);
        log.info("Test accuracy: {}%", accuracy);

        // Step 5: For binary classification, compute AUC and Lift
        Double auc = null;
        ResultSet liftRs = null;
        List<String> classValues = modelHandle.getClassValues();
        if (classValues != null && classValues.size() == 2) {
            String rocTable = "ML_ROC_" + timestamp;
            String liftTable = "ML_LIFT_" + timestamp;
            context.setRocTableName(rocTable);
            context.setLiftTableName(liftTable);

            String positiveClass = classValues.get(1); // Second class as positive

            // Compute ROC/AUC
            try {
                log.info("Computing ROC/AUC for binary classification (positive class: {})", positiveClass);
                mlInterface.computeROC(conn, applyResultTable, testTableName, targetColumn, rocTable, positiveClass);
                auc = mlInterface.getAUC(conn, rocTable);
                log.info("AUC: {}", auc);
            } catch (Exception e) {
                log.warn("Failed to compute ROC/AUC: {}", e.getMessage());
            }

            // Compute Lift
            try {
                log.info("Computing Lift for binary classification");
                mlInterface.computeLift(conn, applyResultTable, testTableName, targetColumn, liftTable, positiveClass);
                liftRs = mlInterface.getLiftResults(conn, liftTable);
                log.info("Lift analysis computed successfully");
            } catch (Exception e) {
                log.warn("Failed to compute Lift: {}", e.getMessage());
            }
        }

        // Step 4 + Build: Get confusion matrix data and build result
        // Outer try-finally guarantees liftRs cleanup even if confusion matrix processing fails
        try {
            DBMSEvaluationResult evalResult;
            try (ResultSet confusionRs = mlInterface.getConfusionMatrixResults(conn, confusionMatrixTable)) {
                evalResult = DBMSEvaluationResult.fromOracleEvaluation(accuracy, confusionRs, auc, context.getTestingDataSize());
            }

            // Add lift data if available
            if (liftRs != null) {
                try {
                    evalResult.populateLiftData(liftRs);
                } catch (Exception e) {
                    log.warn("Failed to parse lift data: {}", e.getMessage());
                }
            }

            return evalResult;
        } finally {
            if (liftRs != null) {
                try { liftRs.close(); } catch (Exception e) { log.debug("Failed to close lift ResultSet", e); }
            }
        }
    }

    /**
     * Evaluates regression model on test data.
     */
    private DBMSEvaluationResult evaluateRegressionProper(
            DatabaseMachineLearningInterface mlInterface,
            DBNConnection conn,
            DBMSModelHandle modelHandle,
            MLTrainingContext context) throws SQLException {

        String modelName = modelHandle.getModelName();
        String testTableName = modelHandle.getTestTableName();
        String targetColumn = context.getFeatureConfig().getLabelColumns().get(0);

        // Get regression metrics on TEST data (not training data)
        log.info("Computing regression metrics on test table: {}", testTableName);
        try (ResultSet metricsRs = mlInterface.getRegressionMetrics(conn, modelName, targetColumn, testTableName)) {
            return DBMSEvaluationResult.fromRegression(metricsRs, context.getTestingDataSize());
        }
    }

    // ==================== Private Helper Methods ====================

    private String prepareDataSource(MLTrainingContext context) throws Exception {
        MLSourceType sourceType = context.getSourceConfig().getSourceType();
        switch (sourceType) {
            case FILE_SYSTEM:
                log.info("Preparing CSV data source");
                return dataManager.createAndLoadStagingTable(context);
            case OBJECT_STORAGE:
                log.info("Preparing cloud object storage data source");
                return createCloudExternalTable(context);
            default:
                return context.getSourceConfig().getTableSourceConfig().getTableName();
        }
    }

    private String createCloudExternalTable(MLTrainingContext context) throws Exception {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String extTableName = "ML_EXT_" + timestamp;
        CloudSourceConfig cloudConfig = context.getSourceConfig().getCloudSourceConfig();

        List<String> columns = cloudConfig.getDiscoveredColumns();
        if (columns == null || columns.isEmpty())
            throw new IllegalStateException(txt("msg.machineLearning.exception.CloudColumnsMissing"));

        Set<String> numericCols = cloudConfig.getNumericColumns();
        String columnList = columns.stream()
                .map(col -> col + (numericCols.contains(col) ? " NUMBER" : " VARCHAR2(4000)"))
                .collect(Collectors.joining(", "));

        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                txt("prc.machineLearning.title.CreatingExternalTable"),
                txt("prc.machineLearning.text.CreatingExternalTable"),
                getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();
                    mlInterface.createCloudExternalTable(
                            conn,
                            extTableName,
                            cloudConfig.getCredentialName(),
                            cloudConfig.getFileUri(),
                            cloudConfig.getDelimiter(),
                            cloudConfig.isHasHeader() ? "1" : "0",
                            columnList
                    );
                });

        context.setStagingTableName(extTableName);
        context.setShouldCleanupStagingTable(true);
        log.info("Created cloud external table: {}", extTableName);
        return extTableName;
    }

    private String createSettingsTable(MLTrainingContext context, String timestamp) throws SQLException {
        String settingsTableName = "ML_SETTINGS_" + timestamp;
        DBMSAlgorithmType algorithmType = DBMSAlgorithmType.fromTrainerType(context.getTrainerType());

        Map<String, String> settings = settingsBuilder.buildSettings(
                context.getTaskType(),
                algorithmType.getId(),
                context.getTrainerConfig()
        );

        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                txt("prc.machineLearning.title.CreatingSettings"),
                txt("prc.machineLearning.text.CreatingModelSettingsTable"),
                getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();
                    mlInterface.createSettingsTable(conn, settingsTableName);
                    for (Map.Entry<String, String> entry : settings.entrySet()) {
                        mlInterface.insertSetting(conn, settingsTableName, entry.getKey(), entry.getValue());
                    }
                });

        log.info("Created settings table: {} with {} settings", settingsTableName, settings.size());

        context.setSettingsTableName(settingsTableName);
        return settingsTableName;
    }

    private String generateModelName(MLTrainingContext context, String timestamp) {
        // Use user-specified model name if provided
        String userModelName = context.getTrainerConfig().getModelName();
        if (userModelName != null && !userModelName.trim().isEmpty()) {
            String modelName = userModelName.trim().toUpperCase();
            // model name is interpolated into the scheduler PL/SQL action - restrict to plain identifiers
            if (!Strings.isAlphanumericWithUnderscore(modelName)) {
                throw new IllegalArgumentException(txt("msg.machineLearning.exception.InvalidModelName", modelName));
            }
            return modelName;
        }

        String baseName;
        String sourceName = extractSourceName(context);
        if (sourceName != null && !sourceName.isEmpty()) {
            baseName = sourceName.toUpperCase() + "_MODEL";
        } else {
            String taskPrefix = context.getTaskType() == MLTaskType.CLASSIFICATION ? "CLS" : "REG";
            baseName = "ML_MODEL_" + taskPrefix + "_" + timestamp;
        }

        // Use Naming utility to generate unique name
        return Naming.nextNumberedIdentifier(baseName, false, this::getExistingModelNames);
    }

    private Set<String> getExistingModelNames() {
        try {
            return DatabaseInterfaceInvoker.load(Priority.HIGH,
                    getProject(),
                    connection.getConnectionId(),
                    conn -> {
                        Set<String> names = new HashSet<>();
                        DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();
                        try (ResultSet rs = mlInterface.getExistingModelNames(conn)) {
                            while (rs.next()) {
                                names.add(rs.getString("MODEL_NAME").toUpperCase());
                            }
                        }
                        return names;
                    });
        } catch (SQLException e) {
            log.warn("Failed to get existing model names: {}", e.getMessage());
            return Set.of();
        }
    }

    private String extractSourceName(MLTrainingContext context) {
        return MLSourceNames.extractBaseName(context.getRequest().getSourceConfig());
    }

    private int getDistinctClassCount(String tableName, String columnName) throws SQLException {
        return DatabaseInterfaceInvoker.load(Priority.HIGH,
                getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();
                    return mlInterface.getDistinctClassCount(conn, columnName, tableName);
                });
    }

    private List<String> getClassValues(String tableName, String columnName) throws SQLException {
        return DatabaseInterfaceInvoker.load(Priority.HIGH,
                getProject(),
                connection.getConnectionId(),
                conn -> {
                    List<String> values = new ArrayList<>();
                    DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();
                    try (ResultSet rs = mlInterface.getClassValues(conn, columnName, tableName)) {
                        while (rs.next()) {
                            values.add(rs.getString("CLASS_VALUE"));
                        }
                    }
                    return values;
                });
    }

    private void dropTableSafe(DatabaseMachineLearningInterface mlInterface, DBNConnection conn, String tableName) {
        try {
            mlInterface.dropTable(conn, tableName);
            log.debug("Dropped table: {}", tableName);
        } catch (Exception e) {
            log.debug("Could not drop table {}: {}", tableName, e.getMessage());
        }
    }

    private Project getProject() {
        return connection.getProject();
    }
}
