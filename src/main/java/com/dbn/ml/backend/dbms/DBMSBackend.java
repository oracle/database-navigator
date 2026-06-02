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
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMachineLearningInterface;
import com.dbn.ml.backend.model.MLModelMetadata;
import com.dbn.ml.backend.model.MLTrainingContext;
import com.dbn.ml.model.MLModelDetails;
import com.dbn.ml.model.MLTaskType;
import com.dbn.ml.model.source.MLSourceNames;
import com.dbn.ml.model.source.MLSourceType;
import com.dbn.ml.model.trainer.MLTrainerConfig;
import com.dbn.object.common.DBObjectUtil;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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

    public DBMSModelHandle train(MLTrainingContext context) throws Exception {
        log.info("Starting DBMS_DATA_MINING training for task: {}", context.getTaskType());
        context.setTrainingStartTime(System.currentTimeMillis());

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());

        // Step 1: Prepare data source (create staging table if CSV)
        String sourceTableName = prepareDataSource(context);

        // Step 2: Split data into training and test sets (Oracle recommended approach)
        String trainTableName = "ML_TRAIN_" + timestamp;
        String testTableName = "ML_TEST_" + timestamp;
        splitData(context, sourceTableName, trainTableName, testTableName);

        // Step 3: Create and populate settings table
        String settingsTableName = createSettingsTable(context, timestamp);

        // Step 4: Generate unique model name
        String modelName = generateModelName(context, timestamp);

        // Step 5: Get mining function and target column
        String miningFunction = DBMSAlgorithmType.getMiningFunction(context.getTaskType());
        String targetColumn = context.getFeatureConfig().getLabelColumns().get(0);

        // Step 5b: Pre-training validation
        DBMSAlgorithmType algorithmType = DBMSAlgorithmType.fromDisplayName(context.getAlgorithmName());
        if (context.getTaskType() == MLTaskType.CLASSIFICATION && algorithmType == DBMSAlgorithmType.LOGISTIC_REGRESSION) {
            int classCount = getDistinctClassCount(trainTableName, targetColumn);
            if (classCount > 2) {
                throw new IllegalArgumentException(
                        "Logistic Regression supports binary classification only. " +
                        "Target column '" + targetColumn + "' has " + classCount + " distinct values. " +
                        "Use Random Forest, Decision Tree, Naive Bayes, SVM, or Neural Network for multi-class problems."
                );
            }
        }

        // Step 6: Create the model using DBMS_DATA_MINING on TRAINING data only
        log.info("Creating model: {} using algorithm: {} on training table: {}",
                modelName, context.getAlgorithmName(), trainTableName);

        String finalModelName = modelName;
        String finalTrainTableName = trainTableName;
        String finalSettingsTableName = settingsTableName;

        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Creating ML Model",
                "Training model " + modelName,
                getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();
                    mlInterface.createModel(
                            conn,
                            finalModelName,
                            miningFunction,
                            finalTrainTableName,
                            "CASE_ID",
                            targetColumn,
                            finalSettingsTableName
                    );
                });

        log.info("Model created successfully: {}", modelName);

        // Notify browser to refresh AI models
        DBObjectUtil.refreshUserObjects(connection.getConnectionId(), DBObjectType.AI_MODEL);

        // Step 7: Create model metadata
        Integer classCount = null;
        Integer outputDimensions = null;
        List<String> classValues = null;

        if (context.getTaskType() == MLTaskType.CLASSIFICATION) {
            classCount = getDistinctClassCount(trainTableName, targetColumn);
            classValues = getClassValues(trainTableName, targetColumn);
        } else {
            outputDimensions = context.getFeatureConfig().getLabelColumns().size();
        }

        MLModelMetadata metadata = MLModelMetadata.builder()
                .featureNames(context.getFeatureConfig().getFeatureColumns())
                .labelNames(context.getFeatureConfig().getLabelColumns())
                .algorithmName(context.getAlgorithmName())
                .classCount(classCount)
                .outputDimensions(outputDimensions)
                .build();

        // Step 8: Create model handle with all table references
        DBMSModelHandle modelHandle = new DBMSModelHandle(
                modelName,
                connection,
                context.getTaskType(),
                metadata,
                trainTableName,
                settingsTableName
        );
        modelHandle.setTestTableName(testTableName);
        modelHandle.setSourceTableName(sourceTableName);
        modelHandle.setClassValues(classValues);

        return modelHandle;
    }

    public DBMSEvaluationResult evaluate(DBMSModelHandle modelHandle, MLTrainingContext context) throws Exception {
        log.info("Evaluating model: {} on test data", modelHandle.getModelName());

        return DatabaseInterfaceInvoker.load(Priority.HIGH,
                "Evaluating Model",
                "Computing evaluation metrics",
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

    public DBMSPredictionResult predict(DBMSModelHandle modelHandle, Map<String, Double> featureValues) throws Exception {
        log.info("Making prediction with model: {}", modelHandle.getModelName());

        return DatabaseInterfaceInvoker.load(Priority.HIGH,
                "Making Prediction",
                "Executing prediction query",
                getProject(),
                connection.getConnectionId(),
                conn -> {
                    if (modelHandle.getTaskType() == MLTaskType.CLASSIFICATION) {
                        return predictClassification(conn, modelHandle, featureValues);
                    } else {
                        return predictRegression(conn, modelHandle, featureValues);
                    }
                });
    }

    public List<String> getAvailableAlgorithms(MLTaskType taskType) {
        return DBMSAlgorithmType.getAlgorithmsForTask(taskType)
                .stream()
                .map(DBMSAlgorithmType::getDisplayName)
                .collect(Collectors.toList());
    }

    public void cleanup(MLTrainingContext context) throws Exception {
        log.info("Cleaning up DBMS resources");

        DatabaseInterfaceInvoker.execute(Priority.LOW,
                "Cleanup",
                "Dropping temporary tables",
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
                "Splitting Data",
                "Creating train/test split",
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
        DBMSEvaluationResult evalResult;
        try (ResultSet confusionRs = mlInterface.getConfusionMatrixResults(conn, confusionMatrixTable)) {
            evalResult = DBMSEvaluationResult.fromOracleEvaluation(accuracy, confusionRs, auc, context.getTestingDataSize());
        }

        // Add lift data if available
        if (liftRs != null) {
            try (ResultSet lr = liftRs) {
                evalResult.populateLiftData(lr);
            } catch (Exception e) {
                log.warn("Failed to parse lift data: {}", e.getMessage());
            }
        }

        return evalResult;
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
            throw new IllegalStateException("No columns discovered for cloud source. Please click 'Load Columns' before training.");

        java.util.Set<String> numericCols = cloudConfig.getNumericColumns();
        String columnList = columns.stream()
                .map(col -> col + (numericCols.contains(col) ? " NUMBER" : " VARCHAR2(4000)"))
                .collect(java.util.stream.Collectors.joining(", "));

        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Creating External Table",
                "Creating external table from cloud storage",
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
        DBMSAlgorithmType algorithmType = DBMSAlgorithmType.fromDisplayName(context.getAlgorithmName());

        Map<String, String> settings = settingsBuilder.buildSettings(
                context.getTaskType(),
                algorithmType.getOracleAlgorithmName(),
                context.getTrainerConfig()
        );

        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Creating Settings",
                "Creating model settings table",
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
            return userModelName.trim().toUpperCase();
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

    private DBMSPredictionResult predictClassification(
            DBNConnection conn,
            DBMSModelHandle modelHandle,
            Map<String, Double> featureValues) throws SQLException {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT PREDICTION(").append(modelHandle.getModelName()).append(" USING ");

        int index = 0;
        for (String featureName : featureValues.keySet()) {
            if (index > 0) sql.append(", ");
            sql.append("? AS ").append(featureName);
            index++;
        }

        sql.append(") AS PREDICTION, PREDICTION_PROBABILITY(").append(modelHandle.getModelName()).append(" USING ");

        index = 0;
        for (String featureName : featureValues.keySet()) {
            if (index > 0) sql.append(", ");
            sql.append("? AS ").append(featureName);
            index++;
        }

        sql.append(") AS PROBABILITY FROM DUAL");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            for (int i = 0; i < 2; i++) {
                for (Double value : featureValues.values()) {
                    stmt.setDouble(paramIndex++, value);
                }
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return DBMSPredictionResult.fromClassification(rs);
            }
        }
    }

    private DBMSPredictionResult predictRegression(
            DBNConnection conn,
            DBMSModelHandle modelHandle,
            Map<String, Double> featureValues) throws SQLException {

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT PREDICTION(").append(modelHandle.getModelName()).append(" USING ");

        int index = 0;
        for (String featureName : featureValues.keySet()) {
            if (index > 0) sql.append(", ");
            sql.append("? AS ").append(featureName);
            index++;
        }

        sql.append(") AS PREDICTION FROM DUAL");

        try (PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            for (Double value : featureValues.values()) {
                stmt.setDouble(paramIndex++, value);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                return DBMSPredictionResult.fromRegression(rs);
            }
        }
    }

    /**
     * Loads model internals from Oracle Model Detail Views.
     * Universal views (DM$VG, DM$VS, DM$VW) are queried for all algorithms.
     * Algorithm-specific views (DM$VD, DM$VL, DM$VP, DM$VV) are queried conditionally.
     * Each query is attempted independently — failures are silently ignored.
     */
    public MLModelDetails loadModelDetails(String modelName, DBMSAlgorithmType algorithmType) {
        MLModelDetails details = new MLModelDetails();

        // Universal views
        Map<String, String> stats = loadModelView(modelName, "Global stats",
                (ml, conn) -> ml.getModelGlobalStats(conn, modelName), rs -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    while (rs.next()) {
                        String name = rs.getString("NAME");
                        String stringVal = rs.getString("STRING_VALUE");
                        double numericVal = rs.getDouble("NUMERIC_VALUE");
                        String display = (stringVal != null && !stringVal.isEmpty())
                                ? stringVal : String.valueOf((long) numericVal);
                        if (name != null) map.put(name, display);
                    }
                    return map;
                });
        if (stats != null) details.setGlobalStats(stats);

        Map<String, String> settings = loadModelView(modelName, "Computed settings",
                (ml, conn) -> ml.getModelComputedSettings(conn, modelName), rs -> {
                    Map<String, String> map = new LinkedHashMap<>();
                    while (rs.next()) {
                        String name = rs.getString("SETTING_NAME");
                        String value = rs.getString("SETTING_VALUE");
                        if (name != null) map.put(name, value != null ? value : "");
                    }
                    return map;
                });
        if (settings != null) details.setComputedSettings(settings);

        List<String> alerts = loadModelView(modelName, "Build alerts",
                (ml, conn) -> ml.getModelAlerts(conn, modelName), rs -> {
                    List<String> list = new ArrayList<>();
                    while (rs.next()) {
                        String text = rs.getString("ERROR_TEXT");
                        if (text != null) list.add(text);
                    }
                    return list;
                });
        if (alerts != null && !alerts.isEmpty()) details.setBuildAlerts(alerts);

        if (algorithmType == null) return details;

        switch (algorithmType) {
            case RANDOM_FOREST:
                List<MLModelDetails.VariableImportance> importance = loadModelView(modelName, "Variable importance",
                        (ml, conn) -> ml.getModelVariableImportance(conn, modelName), rs -> {
                            List<MLModelDetails.VariableImportance> list = new ArrayList<>();
                            while (rs.next()) {
                                String name = rs.getString("ATTRIBUTE_NAME");
                                double score = rs.getDouble("ATTRIBUTE_IMPORTANCE");
                                if (name != null) list.add(new MLModelDetails.VariableImportance(name, score));
                            }
                            return list;
                        });
                if (importance != null && !importance.isEmpty()) details.setVariableImportance(importance);
                break;

            case LOGISTIC_REGRESSION:
            case LINEAR_REGRESSION:
                List<MLModelDetails.GLMCoefficient> glmCoefs = loadModelView(modelName, "GLM coefficients",
                        (ml, conn) -> ml.getModelGLMCoefficients(conn, modelName), rs -> {
                            List<MLModelDetails.GLMCoefficient> list = new ArrayList<>();
                            while (rs.next()) {
                                String attrName = rs.getString("ATTRIBUTE_NAME");
                                String attrValue = rs.getString("ATTRIBUTE_VALUE");
                                double coef = rs.getDouble("COEFFICIENT");
                                double stdErr = rs.getDouble("STD_ERROR");
                                double pVal = rs.getDouble("P_VALUE");
                                if (attrName != null) list.add(new MLModelDetails.GLMCoefficient(attrName, attrValue, coef, stdErr, pVal));
                            }
                            return list;
                        });
                if (glmCoefs != null && !glmCoefs.isEmpty()) details.setGlmCoefficients(glmCoefs);
                break;

            case SVM_CLASSIFICATION:
            case SVM_REGRESSION:
                List<MLModelDetails.SVMCoefficient> svmCoefs = loadModelView(modelName, "SVM coefficients",
                        (ml, conn) -> ml.getModelSVMCoefficients(conn, modelName), rs -> {
                            List<MLModelDetails.SVMCoefficient> list = new ArrayList<>();
                            while (rs.next()) {
                                String attrName = rs.getString("ATTRIBUTE_NAME");
                                String attrValue = rs.getString("ATTRIBUTE_VALUE");
                                String className = rs.getString("CLASS");
                                double coef = rs.getDouble("COEFFICIENT");
                                if (attrName != null) list.add(new MLModelDetails.SVMCoefficient(attrName, attrValue, className, coef));
                            }
                            return list;
                        });
                if (svmCoefs != null && !svmCoefs.isEmpty()) details.setSvmCoefficients(svmCoefs);
                break;

            case DECISION_TREE:
                List<MLModelDetails.TreeSplit> splits = loadModelView(modelName, "Tree splits",
                        (ml, conn) -> ml.getModelTreeSplits(conn, modelName), rs -> {
                            List<MLModelDetails.TreeSplit> list = new ArrayList<>();
                            while (rs.next()) {
                                int node = rs.getInt("NODE");
                                int parent = rs.getInt("PARENT");
                                String attrName = rs.getString("ATTRIBUTE_NAME");
                                String operator = rs.getString("OPERATOR");
                                String value = rs.getString("VALUE");
                                list.add(new MLModelDetails.TreeSplit(node, parent, attrName, operator, value));
                            }
                            return list;
                        });
                if (splits != null && !splits.isEmpty()) details.setTreeSplits(splits);
                break;

            case NAIVE_BAYES:
                List<MLModelDetails.NaiveBayesPrior> priors = loadModelView(modelName, "Naive Bayes priors",
                        (ml, conn) -> ml.getModelNaiveBayesPriors(conn, modelName), rs -> {
                            List<MLModelDetails.NaiveBayesPrior> list = new ArrayList<>();
                            while (rs.next()) {
                                String targetVal = rs.getString("TARGET_VALUE");
                                double prob = rs.getDouble("PRIOR_PROBABILITY");
                                int count = rs.getInt("COUNT");
                                if (targetVal != null) list.add(new MLModelDetails.NaiveBayesPrior(targetVal, prob, count));
                            }
                            return list;
                        });
                if (priors != null && !priors.isEmpty()) details.setNbPriors(priors);

                List<MLModelDetails.NaiveBayesConditional> conditionals = loadModelView(modelName, "Naive Bayes conditionals",
                        (ml, conn) -> ml.getModelNaiveBayesConditionals(conn, modelName), rs -> {
                            List<MLModelDetails.NaiveBayesConditional> list = new ArrayList<>();
                            while (rs.next()) {
                                String targetVal = rs.getString("TARGET_VALUE");
                                String attrName = rs.getString("ATTRIBUTE_NAME");
                                String attrValue = rs.getString("ATTRIBUTE_VALUE");
                                double condProb = rs.getDouble("CONDITIONAL_PROBABILITY");
                                list.add(new MLModelDetails.NaiveBayesConditional(targetVal, attrName, attrValue, condProb));
                            }
                            return list;
                        });
                if (conditionals != null && !conditionals.isEmpty()) details.setNbConditionals(conditionals);
                break;

            default:
                // NEURAL_NETWORK_CLASSIFICATION, NEURAL_NETWORK_REGRESSION — weights not human-interpretable, skip
                break;
        }

        return details;
    }

    @FunctionalInterface
    private interface ResultSetQuery {
        ResultSet query(DatabaseMachineLearningInterface ml, DBNConnection conn) throws SQLException;
    }

    @FunctionalInterface
    private interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }

    /** Runs a single model-view query; silently returns null on failure. */
    private <T> T loadModelView(String modelName, String viewLabel, ResultSetQuery query, ResultSetMapper<T> mapper) {
        try {
            return DatabaseInterfaceInvoker.load(Priority.LOW, getProject(),
                    connection.getConnectionId(), conn -> {
                        DatabaseMachineLearningInterface ml = connection.getInterfaces().getMachineLearningInterface();
                        try (ResultSet rs = query.query(ml, conn)) {
                            return mapper.map(rs);
                        }
                    });
        } catch (Exception e) {
            log.debug("{} not available for model {}: {}", viewLabel, modelName, e.getMessage());
            return null;
        }
    }

    private Project getProject() {
        return connection.getProject();
    }
}
