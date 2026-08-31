/*
 * Copyright 2025 Oracle and/or its affiliates
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
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMachineLearningInterface;
import com.dbn.ml.backend.model.MLTrainingContext;
import com.dbn.ml.model.analysis.MLFeatureImportance;
import com.dbn.ml.model.analysis.MLAttributeContribution;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.nls.NlsResources.txt;

/**
 * Answers two questions about the columns behind a trained model.
 * <ul>
 *   <li>Which columns explain the target - a property of the data, computed with attribute importance</li>
 *   <li>Which columns this model actually used - a property of the model, aggregated from PREDICTION_DETAILS</li>
 * </ul>
 * Kept apart from {@link DBMSBackend} so training and analysis stay separate concerns.
 *
 * @author ayoub allali
 */
@Slf4j
public class DBMSFeatureAnalyzer {
    private final ConnectionHandler connection;

    public DBMSFeatureAnalyzer(ConnectionHandler connection) {
        this.connection = connection;
    }

    /**
     * Ranks the feature columns by how well they explain the target, and profiles each of them.
     * Runs against the training table rather than the raw source, so the profile describes the
     * data the model was actually built on.
     */
    public List<MLFeatureImportance> computeFeatureImportance(MLTrainingContext context) throws SQLException {
        String trainTableName = context.getTrainTableName();
        String targetColumn = context.getFeatureConfig().getLabelColumns().get(0);
        List<String> featureColumns = context.getFeatureConfig().getFeatureColumns();

        // register before the DDL, so cleanup drops the table even if the analysis fails midway
        String importanceTable = MLObjectNames.featureImportanceTable(MLObjectNames.timestamp());
        context.setImportanceTableName(importanceTable);

        return DatabaseInterfaceInvoker.load(Priority.HIGH,
                txt("prc.machineLearning.title.AnalyzingFeatures"),
                txt("prc.machineLearning.text.ComputingFeatureImportance"),
                getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseMachineLearningInterface mlInterface = getInterface();
                    mlInterface.computeAttributeImportance(conn, trainTableName, targetColumn, importanceTable);

                    Map<String, Double> importance = loadImportance(mlInterface, conn, importanceTable);
                    Map<String, ColumnInfo> columns = loadColumns(mlInterface, conn, trainTableName);

                    List<MLFeatureImportance> features = new ArrayList<>();
                    for (String column : featureColumns) {
                        ColumnInfo columnInfo = columns.get(column.toUpperCase());
                        features.add(profileColumn(mlInterface, conn, trainTableName, column,
                                importance.get(column.toUpperCase()), columnInfo));
                    }
                    return features;
                });
    }

    /**
     * Ranks the feature columns by how much they contributed to this model's predictions.
     * <p>
     * PREDICTION_DETAILS reports the top contributing attributes per scored row; averaging the
     * absolute weights over the test set turns that into a model wide ranking - our own
     * aggregation of a documented per row function, the same technique used to derive global
     * SHAP importance from local explanations. It is not the permutation importance Oracle
     * AutoML computes; there is no SQL API for that. It answers the same question - what did
     * this model actually rely on - more cheaply, in a single pass.
     * <p>
     * Scores every feature on every row (topN = feature count) rather than PREDICTION_DETAILS'
     * default of 5, which would otherwise silently drop columns that were not top-5 on some rows
     * and skew the average toward whichever columns usually are.
     */
    public List<MLAttributeContribution> computeAttributeContribution(DBMSModelHandle modelHandle) throws SQLException {
        String modelName = modelHandle.getModelName();
        String testTableName = modelHandle.getTestTableName();
        int topN = modelHandle.getMetadata().getFeatureNames().size();

        return DatabaseInterfaceInvoker.load(Priority.HIGH,
                txt("prc.machineLearning.title.AnalyzingFeatures"),
                txt("prc.machineLearning.text.ComputingAttributeContribution"),
                getProject(),
                connection.getConnectionId(),
                conn -> {
                    List<MLAttributeContribution> contributions = new ArrayList<>();
                    try (ResultSet rs = getInterface().getAttributeContribution(conn, modelName, testTableName, topN)) {
                        while (rs.next()) {
                            contributions.add(new MLAttributeContribution(
                                    rs.getString("ATTRIBUTE_NAME"),
                                    rs.getDouble("CONTRIBUTION"),
                                    rs.getLong("OCCURRENCES")));
                        }
                    }
                    return contributions;
                });
    }

    private Map<String, Double> loadImportance(
            DatabaseMachineLearningInterface mlInterface,
            DBNConnection conn,
            String importanceTable) throws SQLException {

        Map<String, Double> importance = new HashMap<>();
        try (ResultSet rs = mlInterface.getAttributeImportance(conn, importanceTable)) {
            while (rs.next()) {
                importance.put(rs.getString("ATTRIBUTE_NAME"), rs.getDouble("EXPLANATORY_VALUE"));
            }
        }
        return importance;
    }

    /**
     * Column metadata of the training table, keyed by upper case name. The name is taken from the
     * table itself rather than from the request, because column names given in a CSV header keep
     * their original case there while the staging table is created with unquoted (upper cased) names.
     */
    private Map<String, ColumnInfo> loadColumns(
            DatabaseMachineLearningInterface mlInterface,
            DBNConnection conn,
            String tableName) throws SQLException {

        Map<String, ColumnInfo> columns = new HashMap<>();
        try (ResultSet rs = mlInterface.getTableColumnTypes(conn, tableName)) {
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                String name = metaData.getColumnName(i);
                columns.put(name.toUpperCase(),
                        new ColumnInfo(name, metaData.getColumnTypeName(i), isNumeric(metaData.getColumnType(i))));
            }
        }
        return columns;
    }

    /**
     * Reads the statistics of a single column. Mean and standard deviation only apply to
     * numeric columns, so text and date columns get the cardinality query instead.
     */
    private MLFeatureImportance profileColumn(
            DatabaseMachineLearningInterface mlInterface,
            DBNConnection conn,
            String tableName,
            String columnName,
            Double importance,
            ColumnInfo columnInfo) throws SQLException {

        // the column is gone from the training table - report what we know and skip the statistics
        if (columnInfo == null) {
            return new MLFeatureImportance(columnName, importance, null, null, null, null, null, null);
        }

        boolean numeric = columnInfo.numeric();
        String identifier = columnInfo.name();

        try (ResultSet rs = numeric ?
                mlInterface.getColumnStatistics(conn, tableName, identifier) :
                mlInterface.getColumnCardinality(conn, tableName, identifier)) {

            if (!rs.next()) {
                return new MLFeatureImportance(columnName, importance, columnInfo.typeName(), null, null, null, null, null);
            }

            String minValue = rs.getString("MIN_VALUE");
            String maxValue = rs.getString("MAX_VALUE");
            if (numeric) {
                minValue = withLeadingZero(minValue);
                maxValue = withLeadingZero(maxValue);
            }

            return new MLFeatureImportance(
                    columnName,
                    importance,
                    columnInfo.typeName(),
                    rs.getLong("DISTINCT_VALUES"),
                    minValue,
                    maxValue,
                    numeric ? nullableDouble(rs, "MEAN_VALUE") : null,
                    numeric ? nullableDouble(rs, "STD_DEV") : null);
        }
    }

    /**
     * Oracle's default TO_CHAR(number) drops the leading zero of a fraction between -1 and 1
     * (0.1 renders as ".1", -0.1 as "-.1"). Restores it without touching the rest of the value,
     * so integers and larger numbers keep Oracle's natural precision untouched.
     */
    private static @Nullable String withLeadingZero(@Nullable String value) {
        if (value == null) return null;
        if (value.startsWith(".")) return "0" + value;
        if (value.startsWith("-.")) return "-0" + value.substring(1);
        return value;
    }

    private static Double nullableDouble(ResultSet rs, @NonNls String columnName) throws SQLException {
        double value = rs.getDouble(columnName);
        return rs.wasNull() ? null : value;
    }

    private static boolean isNumeric(int sqlType) {
        return sqlType == Types.NUMERIC || sqlType == Types.DECIMAL || sqlType == Types.INTEGER
                || sqlType == Types.BIGINT || sqlType == Types.SMALLINT || sqlType == Types.TINYINT
                || sqlType == Types.DOUBLE || sqlType == Types.FLOAT || sqlType == Types.REAL;
    }

    private DatabaseMachineLearningInterface getInterface() {
        return connection.getInterfaces().getMachineLearningInterface();
    }

    private Project getProject() {
        return connection.getProject();
    }

    private record ColumnInfo(String name, String typeName, boolean numeric) {}
}
