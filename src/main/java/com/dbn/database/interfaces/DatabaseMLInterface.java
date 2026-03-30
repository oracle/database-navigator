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

package com.dbn.database.interfaces;

import com.dbn.connection.jdbc.DBNConnection;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Database interface for Oracle DBMS_DATA_MINING operations.
 * Provides methods for training, evaluating, and managing ML models in the database.
 *
 * @author Oracle
 */
public interface DatabaseMLInterface extends DatabaseInterface {

    // ==================== MODEL CREATION ====================

    /**
     * Creates a classification or regression model using DBMS_DATA_MINING.
     */
    void createModel(
            DBNConnection conn,
            String modelName,
            String miningFunction,
            String dataTableName,
            String caseIdColumn,
            String targetColumn,
            String settingsTableName
    ) throws SQLException;

    /**
     * Creates a settings table for DBMS_DATA_MINING.
     */
    void createSettingsTable(DBNConnection conn, String settingsTableName) throws SQLException;

    /**
     * Inserts a setting into the settings table.
     */
    void insertSetting(
            DBNConnection conn,
            String settingsTableName,
            String settingName,
            String settingValue
    ) throws SQLException;

    /**
     * Drops a model.
     */
    void dropModel(DBNConnection conn, String modelName) throws SQLException;

    /**
     * Renames a model using DBMS_DATA_MINING.RENAME_MODEL.
     */
    void renameModel(DBNConnection conn, String oldModelName, String newModelName) throws SQLException;

    // ==================== DATA SPLITTING ====================

    /**
     * Creates a training data table by sampling from source table.
     * Uses Oracle's SAMPLE clause with SEED for reproducibility.
     *
     * @param conn Database connection
     * @param trainTableName Name for the training table
     * @param sourceTableName Source data table
     * @param samplePercent Percentage of data for training (e.g., 80)
     * @param seed Random seed for reproducibility
     */
    void createTrainingTable(
            DBNConnection conn,
            String trainTableName,
            String sourceTableName,
            int samplePercent,
            long seed
    ) throws SQLException;

    /**
     * Creates a test data table containing rows not in training table.
     * Uses MINUS operation: source MINUS training = test.
     *
     * @param conn Database connection
     * @param testTableName Name for the test table
     * @param sourceTableName Source data table
     * @param trainTableName Training table (to exclude)
     */
    void createTestTable(
            DBNConnection conn,
            String testTableName,
            String sourceTableName,
            String trainTableName
    ) throws SQLException;

    /**
     * Adds a CASE_ID identity column to a table.
     * Required for COMPUTE_CONFUSION_MATRIX and other evaluation procedures.
     */
    void addCaseIdColumn(DBNConnection conn, String tableName) throws SQLException;

    /**
     * Gets the row count from a table.
     */
    int getRowCount(DBNConnection conn, String tableName) throws SQLException;

    // ==================== MODEL APPLICATION ====================

    /**
     * Makes an ad-hoc prediction using a trained model.
     *
     * @param conn Database connection
     * @param modelName The trained model name
     * @param featureClause SQL clause with feature values (e.g., "1.5 AS col1, 'A' AS col2")
     * @return The predicted value as a string
     */
    String predict(DBNConnection conn, String modelName, String featureClause) throws SQLException;

    /**
     * Makes an ad-hoc prediction with probability using a trained model.
     * For classification models only.
     *
     * @param conn Database connection
     * @param modelName The trained model name
     * @param featureClause SQL clause with feature values (e.g., "1.5 AS col1, 'A' AS col2")
     * @return ResultSet with PREDICTION and PROBABILITY columns
     */
    ResultSet predictWithProbability(DBNConnection conn, String modelName, String featureClause) throws SQLException;

    /**
     * Creates an apply results table with predictions for classification.
     * Contains CASE_ID, PREDICTION, and PROBABILITY columns.
     */
    void createApplyResults(
            DBNConnection conn,
            String applyResultTableName,
            String modelName,
            String testTableName
    ) throws SQLException;

    /**
     * Creates an apply results table with predictions for regression.
     * Contains CASE_ID and PREDICTION columns (no probability).
     */
    void createApplyResultsRegression(
            DBNConnection conn,
            String applyResultTableName,
            String modelName,
            String testTableName
    ) throws SQLException;

    // ==================== EVALUATION PROCEDURES (Oracle DBMS_DATA_MINING) ====================

    /**
     * Computes confusion matrix using DBMS_DATA_MINING.COMPUTE_CONFUSION_MATRIX.
     * Creates a confusion matrix table and stores accuracy in a temp table.
     *
     * @param conn Database connection
     * @param applyResultTableName Table with predictions (from createApplyResults)
     * @param targetTableName Table with actual values (test data)
     * @param targetColumn Target column name
     * @param confusionMatrixTableName Name for the confusion matrix output table
     */
    void computeConfusionMatrix(
            DBNConnection conn,
            String applyResultTableName,
            String targetTableName,
            String targetColumn,
            String confusionMatrixTableName
    ) throws SQLException;

    /**
     * Gets the accuracy value computed by computeConfusionMatrix.
     */
    double getAccuracy(DBNConnection conn, String confusionMatrixTableName) throws SQLException;

    /**
     * Gets the confusion matrix results.
     *
     * @return ResultSet with columns: ACTUAL_TARGET_VALUE, PREDICTED_TARGET_VALUE, VALUE
     */
    ResultSet getConfusionMatrixResults(DBNConnection conn, String confusionMatrixTableName) throws SQLException;

    /**
     * Computes ROC curve and AUC using DBMS_DATA_MINING.COMPUTE_ROC.
     * Only for binary classification.
     *
     * @param positiveTargetValue The value representing the positive class
     */
    void computeROC(
            DBNConnection conn,
            String applyResultTableName,
            String targetTableName,
            String targetColumn,
            String rocTableName,
            String positiveTargetValue
    ) throws SQLException;

    /**
     * Gets the AUC (Area Under Curve) computed by computeROC.
     */
    double getAUC(DBNConnection conn, String rocTableName) throws SQLException;

    /**
     * Computes Lift using DBMS_DATA_MINING.COMPUTE_LIFT.
     * Only for binary classification.
     *
     * @param positiveTargetValue The value representing the positive class
     */
    void computeLift(
            DBNConnection conn,
            String applyResultTableName,
            String targetTableName,
            String targetColumn,
            String liftTableName,
            String positiveTargetValue
    ) throws SQLException;

    /**
     * Gets the lift results.
     *
     * @return ResultSet with columns: QUANTILE_NUMBER, PROBABILITY_THRESHOLD, GAIN_CUMULATIVE, LIFT_CUMULATIVE, etc.
     */
    ResultSet getLiftResults(DBNConnection conn, String liftTableName) throws SQLException;

    // ==================== LEGACY EVALUATION (backward compatibility) ====================

    /**
     * Retrieves classification accuracy by applying model to data (legacy method).
     */
    ResultSet getClassificationAccuracy(
            DBNConnection conn,
            String modelName,
            String targetColumn,
            String dataTableName
    ) throws SQLException;

    /**
     * Retrieves confusion matrix by applying model to data (legacy method).
     */
    ResultSet getConfusionMatrix(
            DBNConnection conn,
            String modelName,
            String targetColumn,
            String dataTableName
    ) throws SQLException;

    /**
     * Retrieves regression metrics by applying model to data.
     */
    ResultSet getRegressionMetrics(
            DBNConnection conn,
            String modelName,
            String targetColumn,
            String dataTableName
    ) throws SQLException;

    // ==================== MODEL DETAIL VIEWS ====================

    /**
     * Queries the global statistics view (DM$VG) for a model.
     * Returns general model information: convergence, iterations, row count, etc.
     *
     * @return ResultSet with columns: NAME, NUMERIC_VALUE, STRING_VALUE
     */
    ResultSet getModelGlobalStats(DBNConnection conn, String modelName) throws SQLException;

    /**
     * Queries the attribute importance / detail view (DM$VA) for a model.
     * Returns variable importance for algorithms that support it (RF, EM, NMF, etc.)
     * or model-specific attribute details (GLM coefficients, NN weights, etc.).
     *
     * @return ResultSet with algorithm-specific columns
     */
    ResultSet getModelAttributeDetails(DBNConnection conn, String modelName) throws SQLException;

    /**
     * Queries variable importance from DM$VA (ATTRIBUTE_NAME, ATTRIBUTE_IMPORTANCE).
     * Supported by Random Forest, MDL/Attribute Importance, and EM algorithms.
     * Throws SQLException for algorithms that don't support this view structure.
     *
     * @return ResultSet with columns: ATTRIBUTE_NAME, ATTRIBUTE_IMPORTANCE
     */
    ResultSet getModelVariableImportance(DBNConnection conn, String modelName) throws SQLException;

    /**
     * Queries the computed settings view (DM$VS) for a model.
     * Returns the actual settings used to build the model.
     *
     * @return ResultSet with columns: SETTING_NAME, SETTING_VALUE
     */
    ResultSet getModelComputedSettings(DBNConnection conn, String modelName) throws SQLException;

    /**
     * Queries the build alerts view (DM$VW) for a model — all algorithms.
     *
     * @return ResultSet with columns: ERROR_NUMBER, ERROR_TEXT
     */
    ResultSet getModelAlerts(DBNConnection conn, String modelName) throws SQLException;

    /**
     * Queries GLM coefficients (DM$VD) — Logistic Regression and Linear Regression.
     *
     * @return ResultSet with columns: ATTRIBUTE_NAME, ATTRIBUTE_VALUE, COEFFICIENT, STD_ERROR, TEST_STATISTIC, P_VALUE, VIF
     */
    ResultSet getModelGLMCoefficients(DBNConnection conn, String modelName) throws SQLException;

    /**
     * Queries SVM linear coefficients (DM$VL) — SVM Classification and SVM Regression.
     *
     * @return ResultSet with columns: ATTRIBUTE_NAME, ATTRIBUTE_VALUE, COEFFICIENT
     */
    ResultSet getModelSVMCoefficients(DBNConnection conn, String modelName) throws SQLException;

    /**
     * Queries Decision Tree splits (DM$VP) — Decision Tree only.
     *
     * @return ResultSet with columns: NODE, PARENT, ATTRIBUTE_NAME, OPERATOR, VALUE
     */
    ResultSet getModelTreeSplits(DBNConnection conn, String modelName) throws SQLException;

    /**
     * Queries Naive Bayes class priors (DM$VP) — Naive Bayes only.
     *
     * @return ResultSet with columns: TARGET_VALUE, PRIOR_PROBABILITY, COUNT
     */
    ResultSet getModelNaiveBayesPriors(DBNConnection conn, String modelName) throws SQLException;

    /**
     * Queries Naive Bayes top conditional probabilities (DM$VV) — Naive Bayes only.
     *
     * @return ResultSet with columns: TARGET_VALUE, ATTRIBUTE_NAME, ATTRIBUTE_VALUE, CONDITIONAL_PROBABILITY
     */
    ResultSet getModelNaiveBayesConditionals(DBNConnection conn, String modelName) throws SQLException;

    // ==================== CLOUD OBJECT STORAGE ====================

    /**
     * Creates an external table over a cloud object storage CSV file using DBMS_CLOUD.
     *
     * @param conn Database connection
     * @param tableName Name for the external table
     * @param credentialName DB credential name (null for PAR/presigned URLs)
     * @param fileUri Cloud file URI (https://)
     * @param delimiter CSV delimiter
     * @param skipHeaders "1" to skip header row, "0" otherwise
     * @param columnList  DDL-style column definitions e.g. "COL1 VARCHAR2(4000), COL2 VARCHAR2(4000)"
     */
    void createCloudExternalTable(
            DBNConnection conn,
            String tableName,
            String credentialName,
            String fileUri,
            String delimiter,
            String skipHeaders,
            String columnList
    ) throws SQLException;

    /**
     * Reads the first bytes of a cloud CSV file using DBMS_CLOUD.GET_OBJECT
     * and returns the raw text head (up to 4000 chars).
     * The caller is responsible for extracting the first line and splitting by delimiter.
     *
     * @param conn Database connection
     * @param credentialName DB credential name
     * @param fileUri Cloud file URI (https://)
     * @return The first portion of the file as a string
     */
    String getCloudCsvHeader(DBNConnection conn, String credentialName, String fileUri) throws SQLException;

    // ==================== UTILITY OPERATIONS ====================

    /**
     * Creates a staging table for CSV data.
     */
    void createStagingTable(
            DBNConnection conn,
            String schemaName,
            String tableName,
            String columnDefinitions
    ) throws SQLException;

    /**
     * Drops a staging table.
     */
    void dropStagingTable(DBNConnection conn, String schemaName, String tableName) throws SQLException;

    /**
     * Drops a table (generic).
     */
    void dropTable(DBNConnection conn, String tableName) throws SQLException;

    /**
     * Checks if a table exists.
     */
    boolean tableExists(DBNConnection conn, String schemaName, String tableName) throws SQLException;

    /**
     * Gets the distinct class count from a column.
     */
    int getDistinctClassCount(DBNConnection conn, String columnName, String tableName) throws SQLException;

    /**
     * Gets the distinct class values from a column.
     *
     * @return ResultSet with column CLASS_VALUE
     */
    ResultSet getClassValues(DBNConnection conn, String columnName, String tableName) throws SQLException;

    /**
     * Gets existing model names from USER_MINING_MODELS.
     *
     * @return ResultSet with column MODEL_NAME
     */
    ResultSet getExistingModelNames(DBNConnection conn) throws SQLException;

    /**
     * Gets the INPUT feature attributes for an existing model from USER_MINING_MODEL_ATTRIBUTES.
     * Excludes TARGET, CASE_ID, PARTITION, and SUPPLEMENTARY columns.
     *
     * @return ResultSet with column ATTRIBUTE_NAME
     */
    ResultSet getModelInputAttributes(DBNConnection conn, String modelName) throws SQLException;

    /**
     * Gets the mining function (CLASSIFICATION or REGRESSION) for an existing model.
     *
     * @return "CLASSIFICATION" or "REGRESSION", or null if not found
     */
    String getModelFunction(DBNConnection conn, String modelName) throws SQLException;

    // ==================== ASYNC TRAINING (DBMS_SCHEDULER) ====================

    /**
     * Submits a CREATE_MODEL call as a DBMS_SCHEDULER job so training
     * continues on the DB server even after the client disconnects.
     *
     * @param jobName   Unique scheduler job name (max 30 chars)
     * @param jobAction PL/SQL anonymous block: "BEGIN DBMS_DATA_MINING.CREATE_MODEL(...); END;"
     */
    void submitTrainingJob(DBNConnection conn, String jobName, String jobAction) throws SQLException;

    /**
     * Returns the STATE of a scheduler job: SCHEDULED, RUNNING, SUCCEEDED, FAILED, or null if not found.
     */
    String getSchedulerJobState(DBNConnection conn, String jobName) throws SQLException;

    /**
     * Returns the final run STATUS from USER_SCHEDULER_JOB_RUN_DETAILS: SUCCEEDED or FAILED.
     * Returns null if the job has not run yet or the log entry is unavailable.
     */
    String getSchedulerJobRunStatus(DBNConnection conn, String jobName) throws SQLException;

    /**
     * Drops a completed DBMS_SCHEDULER job to clean up the job registry.
     */
    void dropSchedulerJob(DBNConnection conn, String jobName) throws SQLException;
}
