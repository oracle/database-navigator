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

package com.dbn.database.oracle;

import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.database.common.DatabaseInterfaceBase;
import com.dbn.database.interfaces.DatabaseInterfaceType;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.interfaces.DatabaseMachineLearningInterface;
import com.dbn.ml.backend.model.MLPredictionAttribute;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.StringJoiner;

import static com.dbn.language.common.quotes.QuoteEscaping.DATABASE;

/**
 * Oracle implementation of DatabaseMLInterface.
 * Provides access to DBMS_DATA_MINING package for in-database ML training.
 *
 * @author Oracle
 */
@Slf4j
public class OracleMachineLearningInterface extends DatabaseInterfaceBase implements DatabaseMachineLearningInterface {
    private static final int CLOUD_CSV_SAMPLE_BYTES = 1024 * 1024;
    private static final int PREDICTION_QUERY_TIMEOUT = 30000;

    public OracleMachineLearningInterface(DatabaseInterfaces provider) {
        super("oracle_ml_interface.xml", provider);
    }


    @Override
    public DatabaseInterfaceType getInterfaceType() {
        return DatabaseInterfaceType.MACHINE_LEARNING;
    }
    // ==================== MODEL CREATION ====================

    @Override
    public void createSettingsTable(DBNConnection conn, String settingsTableName) throws SQLException {
        log.debug("Creating settings table: {}", settingsTableName);
        executeUpdate(conn, "create-settings-table", settingsTableName);
    }

    @Override
    public void insertSetting(
            DBNConnection conn,
            String settingsTableName,
            String settingName,
            String settingValue
    ) throws SQLException {
        executeUpdate(conn, "insert-setting", settingsTableName, settingName, settingValue);
    }

    @Override
    public void dropModel(DBNConnection conn, String modelName) throws SQLException {
        log.debug("Dropping ML model: {}", modelName);
        executeUpdate(conn, "drop-ml-model", modelName);
    }

    @Override
    public void renameModel(DBNConnection conn, String oldModelName, String newModelName) throws SQLException {
        log.debug("Renaming ML model: {} -> {}", oldModelName, newModelName);
        executeUpdate(conn, "rename-ml-model", oldModelName, newModelName);
    }

    // ==================== DATA SPLITTING ====================

    @Override
    public void createTrainingTable(
            DBNConnection conn,
            String trainTableName,
            String sourceSchemaName,
            String sourceTableName,
            int samplePercent,
            long seed
    ) throws SQLException {
        log.debug("Creating training table: {} from {}.{} ({}% with seed {})",
                trainTableName, sourceSchemaName, sourceTableName, samplePercent, seed);
        executeUpdate(conn, "create-training-table",
                trainTableName, sourceSchemaName, sourceTableName, samplePercent, seed);
    }

    @Override
    public void createTestTable(
            DBNConnection conn,
            String testTableName,
            String sourceSchemaName,
            String sourceTableName,
            String trainTableName
    ) throws SQLException {
        log.debug("Creating test table: {} (source {}.{} MINUS training {})",
                testTableName, sourceSchemaName, sourceTableName, trainTableName);
        executeUpdate(conn, "create-test-table", testTableName, sourceSchemaName, sourceTableName, trainTableName);
    }

    @Override
    public void addCaseIdColumn(DBNConnection conn, String tableName) throws SQLException {
        log.debug("Adding CASE_ID column to table: {}", tableName);
        executeUpdate(conn, "add-case-id-column", tableName);
    }

    @Override
    public int getRowCount(DBNConnection conn, String tableName) throws SQLException {
        try (ResultSet rs = executeQuery(conn, "get-row-count", tableName)) {
            if (rs.next()) {
                return rs.getInt("ROW_COUNT");
            }
            return 0;
        }
    }

    // ==================== MODEL APPLICATION ====================

    @Override
    public @NonNls String buildPredictionStatement(
            DBNConnection conn,
            @NonNls String modelName,
            List<MLPredictionAttribute> attributes,
            boolean withProbability) {

        String quotedModelName = getIdentifierEnquoter(conn).quote(modelName, DATABASE);
        StringJoiner inputs = new StringJoiner(",\n       ");
        for (MLPredictionAttribute attribute : attributes) {
            String quotedAttributeName = getIdentifierEnquoter(conn).quote(attribute.getName(), DATABASE);
            inputs.add("? AS " + quotedAttributeName);
        }

        String probability = withProbability ?
                ",\n    PREDICTION_PROBABILITY(" + quotedModelName + " USING *) AS PROBABILITY" : "";
        return "SELECT\n" +
                "    PREDICTION(" + quotedModelName + " USING *) AS PREDICTION" + probability + "\n" +
                "FROM (\n" +
                "    SELECT " + inputs + "\n" +
                "    FROM DUAL\n" +
                ")";
    }

    @Override
    public DBNResultSet predict(
            DBNConnection conn,
            String modelName,
            List<MLPredictionAttribute> attributes,
            List<Object> values,
            boolean withProbability) throws SQLException {

        if (attributes.size() != values.size()) {
            throw new IllegalArgumentException("Each prediction attribute must have a value");
        }

        log.debug("Ad-hoc prediction using model: {}", modelName);
        String statementText = buildPredictionStatement(conn, modelName, attributes, withProbability);
        DBNPreparedStatement<?> statement = conn.prepareStatement(statementText);
        try {
            statement.setQueryTimeout(PREDICTION_QUERY_TIMEOUT);
            for (int i = 0; i < values.size(); i++) {
                Object value = values.get(i);
                MLPredictionAttribute attribute = attributes.get(i);
                if (value == null) {
                    statement.setNull(i + 1, attribute.getJdbcType());
                } else {
                    statement.setObject(i + 1, value);
                }
            }

            return statement.executeQuery();
        } catch (SQLException | RuntimeException e) {
            Resources.close(statement);
            throw e;
        }
    }

    @Override
    public void createApplyResults(
            DBNConnection conn,
            String applyResultTableName,
            String modelName,
            String testTableName
    ) throws SQLException {
        log.debug("Creating apply results table: {} for model {} on {}",
                applyResultTableName, modelName, testTableName);
        executeUpdate(conn, "create-apply-results", applyResultTableName, modelName, testTableName);
    }

    @Override
    public void createApplyResultsRegression(
            DBNConnection conn,
            String applyResultTableName,
            String modelName,
            String testTableName
    ) throws SQLException {
        log.debug("Creating regression apply results table: {} for model {} on {}",
                applyResultTableName, modelName, testTableName);
        executeUpdate(conn, "create-apply-results-regression", applyResultTableName, modelName, testTableName);
    }

    // ==================== EVALUATION PROCEDURES ====================

    @Override
    public void computeConfusionMatrix(
            DBNConnection conn,
            String applyResultTableName,
            String targetTableName,
            String targetColumn,
            String confusionMatrixTableName,
            String accuracyTableName
    ) throws SQLException {
        log.debug("Computing confusion matrix: {} from apply={}, target={}",
                confusionMatrixTableName, applyResultTableName, targetTableName);
        executeUpdate(conn, "compute-confusion-matrix",
                applyResultTableName, targetTableName, targetColumn, confusionMatrixTableName, accuracyTableName);
    }

    @Override
    public double getAccuracy(DBNConnection conn, String accuracyTableName) throws SQLException {
        try (ResultSet rs = executeQuery(conn, "get-accuracy", accuracyTableName)) {
            if (rs.next()) {
                return rs.getDouble("ACCURACY");
            }
            return 0.0;
        }
    }

    @Override
    public ResultSet getConfusionMatrixResults(DBNConnection conn, String confusionMatrixTableName) throws SQLException {
        return executeQuery(conn, "get-confusion-matrix-results", confusionMatrixTableName);
    }

    @Override
    public void computeROC(
            DBNConnection conn,
            String applyResultTableName,
            String targetTableName,
            String targetColumn,
            String rocTableName,
            String aucTableName,
            String positiveTargetValue
    ) throws SQLException {
        log.debug("Computing ROC: {} for positive class '{}'", rocTableName, positiveTargetValue);
        executeUpdate(conn, "compute-roc",
                applyResultTableName, targetTableName, targetColumn, rocTableName, aucTableName, positiveTargetValue);
    }

    @Override
    public double getAUC(DBNConnection conn, String aucTableName) throws SQLException {
        try (ResultSet rs = executeQuery(conn, "get-auc", aucTableName)) {
            if (rs.next()) {
                return rs.getDouble("AUC");
            }
            return 0.0;
        }
    }

    @Override
    public void computeLift(
            DBNConnection conn,
            String applyResultTableName,
            String targetTableName,
            String targetColumn,
            String liftTableName,
            String positiveTargetValue
    ) throws SQLException {
        log.debug("Computing Lift: {} for positive class '{}'", liftTableName, positiveTargetValue);
        executeUpdate(conn, "compute-lift",
                applyResultTableName, targetTableName, targetColumn, liftTableName, positiveTargetValue);
    }

    @Override
    public ResultSet getLiftResults(DBNConnection conn, String liftTableName) throws SQLException {
        return executeQuery(conn, "get-lift-results", liftTableName);
    }

    // ==================== LEGACY EVALUATION ====================

    @Override
    public ResultSet getClassificationAccuracy(
            DBNConnection conn,
            String modelName,
            String targetColumn,
            String dataTableName
    ) throws SQLException {
        return executeQuery(conn, "get-classification-accuracy", modelName, targetColumn, dataTableName);
    }

    @Override
    public ResultSet getConfusionMatrix(
            DBNConnection conn,
            String modelName,
            String targetColumn,
            String dataTableName
    ) throws SQLException {
        return executeQuery(conn, "get-confusion-matrix", modelName, targetColumn, dataTableName);
    }

    @Override
    public ResultSet getRegressionMetrics(
            DBNConnection conn,
            String modelName,
            String targetColumn,
            String dataTableName
    ) throws SQLException {
        return executeQuery(conn, "get-regression-metrics", modelName, targetColumn, dataTableName);
    }

    // ==================== MODEL DETAIL VIEWS ====================

    @Override
    public ResultSet getModelGlobalStats(DBNConnection conn, String modelName) throws SQLException {
        log.debug("Querying model global stats (DM$VG) for: {}", modelName);
        return executeQuery(conn, "get-model-global-stats", modelName);
    }

    @Override
    public ResultSet getModelAttributeDetails(DBNConnection conn, String modelName) throws SQLException {
        log.debug("Querying model attribute details (DM$VA) for: {}", modelName);
        return executeQuery(conn, "get-model-attribute-details", modelName);
    }

    @Override
    public ResultSet getModelComputedSettings(DBNConnection conn, String modelName) throws SQLException {
        log.debug("Querying model computed settings (DM$VS) for: {}", modelName);
        return executeQuery(conn, "get-model-computed-settings", modelName);
    }

    @Override
    public ResultSet getModelAlerts(DBNConnection conn, String modelName) throws SQLException {
        log.debug("Querying model build alerts (DM$VW) for: {}", modelName);
        return executeQuery(conn, "get-model-alerts", modelName);
    }

    @Override
    public ResultSet getModelDetailViews(DBNConnection conn, String modelName) throws SQLException {
        log.debug("Querying model detail views for: {}", modelName);
        return executeQuery(conn, "get-model-detail-views", modelName);
    }

    @Override
    public ResultSet getModelGLMCoefficients(DBNConnection conn, String modelName) throws SQLException {
        log.debug("Querying GLM coefficients (DM$VD) for: {}", modelName);
        return executeQuery(conn, "get-model-glm-coefficients", modelName);
    }

    @Override
    public ResultSet getModelSVMCoefficients(DBNConnection conn, String modelName) throws SQLException {
        log.debug("Querying SVM coefficients (DM$VL) for: {}", modelName);
        return executeQuery(conn, "get-model-svm-coefficients", modelName);
    }

    @Override
    public ResultSet getModelTreeSplits(DBNConnection conn, String modelName) throws SQLException {
        log.debug("Querying Decision Tree splits (DM$VP) for: {}", modelName);
        return executeQuery(conn, "get-model-tree-splits", modelName);
    }

    @Override
    public ResultSet getModelNaiveBayesPriors(DBNConnection conn, String modelName) throws SQLException {
        log.debug("Querying Naive Bayes priors (DM$VP) for: {}", modelName);
        return executeQuery(conn, "get-model-nb-priors", modelName);
    }

    @Override
    public ResultSet getModelNaiveBayesConditionals(DBNConnection conn, String modelName) throws SQLException {
        log.debug("Querying Naive Bayes conditionals (DM$VV) for: {}", modelName);
        return executeQuery(conn, "get-model-nb-conditionals", modelName);
    }

    // ==================== CLOUD OBJECT STORAGE ====================

    @Override
    public void createCloudExternalTable(
            DBNConnection conn,
            String tableName,
            String credentialName,
            String fileUri,
            String delimiter,
            String skipHeaders,
            String columnList
    ) throws SQLException {
        log.debug("Creating cloud external table: {} from URI: {}", tableName, fileUri);
        executeUpdate(conn, "create-cloud-external-table",
                tableName, credentialName, fileUri, delimiter, skipHeaders, columnList);
    }

    @Override
    public String getCloudCsvSample(DBNConnection conn, String credentialName, String fileUri) throws SQLException {
        log.debug("Reading cloud CSV sample from URI: {}", fileUri);
        try (ResultSet rs = executeQuery(conn, "get-cloud-csv-sample", credentialName, fileUri)) {
            if (rs.next()) {
                Blob content = rs.getBlob("FILE_CONTENT");
                if (content == null) return null;

                try (InputStream input = content.getBinaryStream()) {
                    return new String(input.readNBytes(CLOUD_CSV_SAMPLE_BYTES), StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new SQLException("Failed to read cloud CSV sample", e);
                } finally {
                    content.free();
                }
            }
            return null;
        }
    }

    @Override
    public void validateCloudExternalTable(DBNConnection conn, String tableName) throws SQLException {
        log.debug("Validating cloud external table: {}", tableName);
        executeUpdate(conn, "validate-cloud-external-table", tableName);
    }

    // ==================== UTILITY OPERATIONS ====================

    @Override
    public void createStagingTable(
            DBNConnection conn,
            String schemaName,
            String tableName,
            String columnDefinitions
    ) throws SQLException {
        log.debug("Creating staging table: {}.{}", schemaName, tableName);
        String fullTableName = buildFullTableName(schemaName, tableName);
        executeUpdate(conn, "create-staging-table", fullTableName, columnDefinitions);
    }

    @Override
    public void dropStagingTable(DBNConnection conn, String schemaName, String tableName) throws SQLException {
        log.debug("Dropping staging table: {}.{}", schemaName, tableName);
        String fullTableName = buildFullTableName(schemaName, tableName);
        executeUpdate(conn, "drop-staging-table", fullTableName);
    }

    @Override
    public void dropTable(DBNConnection conn, String tableName) throws SQLException {
        log.debug("Dropping table: {}", tableName);
        executeUpdate(conn, "drop-table", tableName);
    }

    @Override
    public boolean tableExists(DBNConnection conn, String schemaName, String tableName) throws SQLException {
        try (ResultSet rs = executeQuery(conn, "check-table-exists",
                schemaName != null ? schemaName.toUpperCase() : null,
                tableName.toUpperCase())) {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        }
    }

    @Override
    public int getDistinctClassCount(DBNConnection conn, String columnName, String tableName) throws SQLException {
        try (ResultSet rs = executeQuery(conn, "get-distinct-class-count", columnName, tableName)) {
            if (rs.next()) {
                return rs.getInt("CLASS_COUNT");
            }
            return 0;
        }
    }

    @Override
    public ResultSet getClassValues(DBNConnection conn, String columnName, String tableName) throws SQLException {
        return executeQuery(conn, "get-class-values", columnName, tableName);
    }

    @Override
    public ResultSet getExistingModelNames(DBNConnection conn) throws SQLException {
        return executeQuery(conn, "get-existing-model-names");
    }

    @Override
    public ResultSet getModelInputAttributes(DBNConnection conn, String modelName) throws SQLException {
        return executeQuery(conn, "get-model-input-attributes", modelName);
    }

    @Override
    public String getModelFunction(DBNConnection conn, String modelName) throws SQLException {
        try (ResultSet rs = executeQuery(conn, "get-model-function", modelName)) {
            if (rs.next()) return rs.getString("MINING_FUNCTION");
            return null;
        }
    }

    // ==================== ASYNC TRAINING ====================

    @Override
    public String buildCreateModelAction(
            DBNConnection conn,
            String modelName,
            String miningFunction,
            String trainTableName,
            String targetColumn,
            String settingsTableName) throws SQLException {
        return renderStatementText(conn, "create-model-job-action",
                modelName, miningFunction, trainTableName, targetColumn, settingsTableName);
    }

    // ==================== FEATURE ANALYSIS ====================

    @Override
    public void computeAttributeImportance(
            DBNConnection conn,
            String dataTableName,
            String targetColumn,
            String resultTableName) throws SQLException {
        log.debug("Computing attribute importance on {} for target {}", dataTableName, targetColumn);
        executeUpdate(conn, "compute-attribute-importance", dataTableName, targetColumn, resultTableName);
    }

    @Override
    public ResultSet getAttributeImportance(DBNConnection conn, String resultTableName) throws SQLException {
        return executeQuery(conn, "get-attribute-importance", resultTableName);
    }

    @Override
    public ResultSet getTableColumnTypes(DBNConnection conn, String tableName) throws SQLException {
        return executeQuery(conn, "get-table-column-types", tableName);
    }

    @Override
    public ResultSet getColumnStatistics(DBNConnection conn, String tableName, String columnName) throws SQLException {
        return executeQuery(conn, "get-column-statistics", columnName, tableName);
    }

    @Override
    public ResultSet getColumnCardinality(DBNConnection conn, String tableName, String columnName) throws SQLException {
        return executeQuery(conn, "get-column-cardinality", columnName, tableName);
    }

    @Override
    public ResultSet getAttributeContribution(DBNConnection conn, String modelName, String testTableName, int topN) throws SQLException {
        log.debug("Aggregating prediction details of model {} over {} (topN={})", modelName, testTableName, topN);
        return executeQuery(conn, "get-attribute-contribution", modelName, testTableName, topN);
    }

    // ==================== HELPER METHODS ====================

    private String buildFullTableName(String schemaName, String tableName) {
        return (schemaName != null && !schemaName.isEmpty())
                ? schemaName + "." + tableName
                : tableName;
    }
}
