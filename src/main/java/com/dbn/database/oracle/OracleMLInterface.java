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

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.DatabaseInterfaceBase;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.interfaces.DatabaseMLInterface;
import lombok.extern.slf4j.Slf4j;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Oracle implementation of DatabaseMLInterface.
 * Provides access to DBMS_DATA_MINING package for in-database ML training.
 *
 * @author Oracle
 */
@Slf4j
public class OracleMLInterface extends DatabaseInterfaceBase implements DatabaseMLInterface {

    public OracleMLInterface(DatabaseInterfaces provider) {
        super("oracle_ml_interface.xml", provider);
    }

    // ==================== MODEL CREATION ====================

    @Override
    public void createModel(
            DBNConnection conn,
            String modelName,
            String miningFunction,
            String dataTableName,
            String caseIdColumn,
            String targetColumn,
            String settingsTableName
    ) throws SQLException {
        log.debug("Creating ML model: {} with function: {}", modelName, miningFunction);
        executeUpdate(conn, "create-ml-model",
                modelName, miningFunction, dataTableName, caseIdColumn, targetColumn, settingsTableName);
    }

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
            String sourceTableName,
            int samplePercent,
            long seed
    ) throws SQLException {
        log.debug("Creating training table: {} from {} ({}% with seed {})",
                trainTableName, sourceTableName, samplePercent, seed);
        executeUpdate(conn, "create-training-table",
                trainTableName, sourceTableName, String.valueOf(samplePercent), String.valueOf(seed));
    }

    @Override
    public void createTestTable(
            DBNConnection conn,
            String testTableName,
            String sourceTableName,
            String trainTableName
    ) throws SQLException {
        log.debug("Creating test table: {} (source {} MINUS training {})",
                testTableName, sourceTableName, trainTableName);
        executeUpdate(conn, "create-test-table", testTableName, sourceTableName, trainTableName);
    }

    @Override
    public void addCaseIdColumn(DBNConnection conn, String tableName) throws SQLException {
        log.debug("Adding CASE_ID column to table: {}", tableName);
        executeUpdate(conn, "add-case-id-column", tableName);
    }

    @Override
    public int getRowCount(DBNConnection conn, String tableName) throws SQLException {
        ResultSet rs = executeQuery(conn, "get-row-count", tableName);
        try {
            if (rs.next()) {
                return rs.getInt("ROW_COUNT");
            }
            return 0;
        } finally {
            rs.close();
        }
    }

    // ==================== MODEL APPLICATION ====================

    @Override
    public String predict(DBNConnection conn, String modelName, String featureClause) throws SQLException {
        log.debug("Ad-hoc prediction using model: {}", modelName);
        ResultSet rs = executeQuery(conn, "predict-adhoc", modelName, featureClause);
        try {
            if (rs.next()) {
                return rs.getString("PREDICTION");
            }
            return null;
        } finally {
            rs.close();
        }
    }

    @Override
    public ResultSet predictWithProbability(DBNConnection conn, String modelName, String featureClause) throws SQLException {
        log.debug("Ad-hoc prediction with probability using model: {}", modelName);
        return executeQuery(conn, "predict-adhoc-with-probability", modelName, featureClause);
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
            String confusionMatrixTableName
    ) throws SQLException {
        log.debug("Computing confusion matrix: {} from apply={}, target={}",
                confusionMatrixTableName, applyResultTableName, targetTableName);
        executeUpdate(conn, "compute-confusion-matrix",
                applyResultTableName, targetTableName, targetColumn, confusionMatrixTableName);
    }

    @Override
    public double getAccuracy(DBNConnection conn, String confusionMatrixTableName) throws SQLException {
        ResultSet rs = executeQuery(conn, "get-accuracy", confusionMatrixTableName);
        try {
            if (rs.next()) {
                return rs.getDouble("ACCURACY");
            }
            return 0.0;
        } finally {
            rs.close();
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
            String positiveTargetValue
    ) throws SQLException {
        log.debug("Computing ROC: {} for positive class '{}'", rocTableName, positiveTargetValue);
        executeUpdate(conn, "compute-roc",
                applyResultTableName, targetTableName, targetColumn, rocTableName, positiveTargetValue);
    }

    @Override
    public double getAUC(DBNConnection conn, String rocTableName) throws SQLException {
        ResultSet rs = executeQuery(conn, "get-auc", rocTableName);
        try {
            if (rs.next()) {
                return rs.getDouble("AUC");
            }
            return 0.0;
        } finally {
            rs.close();
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
    public ResultSet getModelVariableImportance(DBNConnection conn, String modelName) throws SQLException {
        log.debug("Querying variable importance (DM$VA) for: {}", modelName);
        return executeQuery(conn, "get-model-variable-importance", modelName);
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
    public String getCloudCsvHeader(DBNConnection conn, String credentialName, String fileUri) throws SQLException {
        log.debug("Reading cloud CSV header from URI: {}", fileUri);
        ResultSet rs = executeQuery(conn, "get-cloud-csv-header", credentialName, fileUri);
        try {
            if (rs.next()) {
                return rs.getString("FILE_HEAD");
            }
            return null;
        } finally {
            rs.close();
        }
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
        ResultSet rs = executeQuery(conn, "check-table-exists",
                schemaName != null ? schemaName.toUpperCase() : null,
                tableName.toUpperCase());
        try {
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            return false;
        } finally {
            rs.close();
        }
    }

    @Override
    public int getDistinctClassCount(DBNConnection conn, String columnName, String tableName) throws SQLException {
        ResultSet rs = executeQuery(conn, "get-distinct-class-count", columnName, tableName);
        try {
            if (rs.next()) {
                return rs.getInt("CLASS_COUNT");
            }
            return 0;
        } finally {
            rs.close();
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

    // ==================== HELPER METHODS ====================

    private String buildFullTableName(String schemaName, String tableName) {
        return (schemaName != null && !schemaName.isEmpty())
                ? schemaName + "." + tableName
                : tableName;
    }
}
