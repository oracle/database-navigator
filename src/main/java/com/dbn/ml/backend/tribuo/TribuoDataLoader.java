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

package com.dbn.ml.backend.tribuo;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.ml.model.feature.MLFeatureConfig;
import com.dbn.ml.model.source.MLFileSourceConfig;
import com.dbn.ml.model.source.MLSourceConfig;
import com.dbn.ml.model.source.MLSourceType;
import com.dbn.ml.model.source.MLTableSourceConfig;
import org.tribuo.Example;
import org.tribuo.Output;
import org.tribuo.classification.Label;
import org.tribuo.impl.ArrayExample;
import org.tribuo.regression.Regressor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Data loader for Tribuo backend.
 * Extracts data from database tables or CSV files and converts to Tribuo Examples.
 *
 * @author Oracle
 */
public class TribuoDataLoader {

    /**
     * Load classification data from source.
     */
    public List<Example<Label>> loadClassificationData(
            MLSourceConfig sourceConfig,
            MLFeatureConfig featureConfig,
            ConnectionHandler connectionHandler) throws Exception {

        return loadData(sourceConfig, featureConfig, connectionHandler,
                (featureNames, featureValues, rs, labelColumn) -> {
                    String labelValue = rs.getString(labelColumn);
                    return new ArrayExample<>(new Label(labelValue), featureNames, featureValues);
                },
                (featureNames, featureValues, values, labelIndex) -> {
                    String labelValue = values[labelIndex].trim();
                    return new ArrayExample<>(new Label(labelValue), featureNames, featureValues);
                },
                featureConfig.getLabelColumn()
        );
    }

    /**
     * Load regression data from source.
     */
    public List<Example<Regressor>> loadRegressionData(
            MLSourceConfig sourceConfig,
            MLFeatureConfig featureConfig,
            ConnectionHandler connectionHandler) throws Exception {

        List<String> labelColumns = featureConfig.getLabelColumns();

        return loadRegressionDataInternal(sourceConfig, featureConfig, connectionHandler, labelColumns);
    }

    /**
     * Internal method for regression data loading with proper label column handling.
     */
    private List<Example<Regressor>> loadRegressionDataInternal(
            MLSourceConfig sourceConfig,
            MLFeatureConfig featureConfig,
            ConnectionHandler connectionHandler,
            List<String> labelColumns) throws Exception {

        MLSourceType sourceType = sourceConfig.getSourceType();

        return switch (sourceType) {
            case DATABASE_TABLE -> loadRegressionFromDatabase(
                    sourceConfig.getTableSourceConfig(),
                    featureConfig,
                    connectionHandler,
                    labelColumns
            );
            case FILE_SYSTEM -> loadRegressionFromCSV(
                    sourceConfig.getFileSourceConfig(),
                    featureConfig,
                    labelColumns
            );
            case OBJECT_STORAGE -> throw new UnsupportedOperationException(
                    "Object Storage source not yet implemented"
            );
        };
    }

    /**
     * Load regression data from database.
     */
    private List<Example<Regressor>> loadRegressionFromDatabase(
            MLTableSourceConfig tableConfig,
            MLFeatureConfig featureConfig,
            ConnectionHandler connectionHandler,
            List<String> labelColumns) throws SQLException {

        List<Example<Regressor>> examples = new ArrayList<>();
        String sql = buildQuery(tableConfig, featureConfig);
        List<String> featureColumns = featureConfig.getFeatureColumns();
        String[] featureNames = featureColumns.toArray(new String[0]);

        try (DBNConnection conn = connectionHandler.getMainConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                double[] featureValues = extractFeatureValues(rs, featureColumns);
                Regressor regressor = createRegressorFromResultSet(labelColumns, rs);
                examples.add(new ArrayExample<>(regressor, featureNames, featureValues));
            }
        }

        return examples;
    }

    /**
     * Load regression data from CSV file.
     */
    private List<Example<Regressor>> loadRegressionFromCSV(
            MLFileSourceConfig fileConfig,
            MLFeatureConfig featureConfig,
            List<String> labelColumns) throws Exception {

        List<Example<Regressor>> examples = new ArrayList<>();
        List<String> featureColumns = featureConfig.getFeatureColumns();
        String[] featureNames = featureColumns.toArray(new String[0]);
        String[] labelNames = labelColumns.toArray(new String[0]);

        try (BufferedReader reader = new BufferedReader(new FileReader(fileConfig.getFilePath()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("Empty CSV file");

            String delimiter = fileConfig.getDelimiter();
            String[] headers = headerLine.split(delimiter);

            int[] featureIndices = findColumnIndices(headers, featureColumns);
            int[] labelIndices = findColumnIndices(headers, labelColumns);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(delimiter);
                double[] featureValues = extractFeatureValues(values, featureIndices);
                Regressor regressor = createRegressorFromArray(labelNames, labelIndices, values);
                examples.add(new ArrayExample<>(regressor, featureNames, featureValues));
            }
        }

        return examples;
    }

    /**
     * Generic data loading from any source.
     */
    private <T extends Output<T>> List<Example<T>> loadData(
            MLSourceConfig sourceConfig,
            MLFeatureConfig featureConfig,
            ConnectionHandler connectionHandler,
            DatabaseExampleFactory<T> dbFactory,
            CSVExampleFactory<T> csvFactory,
            String primaryLabelColumn) throws Exception {

        MLSourceType sourceType = sourceConfig.getSourceType();

        return switch (sourceType) {
            case DATABASE_TABLE -> loadFromDatabase(
                    sourceConfig.getTableSourceConfig(),
                    featureConfig,
                    connectionHandler,
                    dbFactory,
                    primaryLabelColumn
            );
            case FILE_SYSTEM -> loadFromCSV(
                    sourceConfig.getFileSourceConfig(),
                    featureConfig,
                    csvFactory,
                    primaryLabelColumn
            );
            case OBJECT_STORAGE -> throw new UnsupportedOperationException(
                    "Object Storage source not yet implemented"
            );
        };
    }

    /**
     * Load data from database table.
     */
    private <T extends Output<T>> List<Example<T>> loadFromDatabase(
            MLTableSourceConfig tableConfig,
            MLFeatureConfig featureConfig,
            ConnectionHandler connectionHandler,
            DatabaseExampleFactory<T> exampleFactory,
            String labelColumn) throws SQLException {

        List<Example<T>> examples = new ArrayList<>();
        String sql = buildQuery(tableConfig, featureConfig);
        List<String> featureColumns = featureConfig.getFeatureColumns();
        String[] featureNames = featureColumns.toArray(new String[0]);

        try (DBNConnection conn = connectionHandler.getMainConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                double[] featureValues = extractFeatureValues(rs, featureColumns);
                Example<T> example = exampleFactory.create(featureNames, featureValues, rs, labelColumn);
                examples.add(example);
            }
        }

        return examples;
    }

    /**
     * Load data from CSV file.
     */
    private <T extends Output<T>> List<Example<T>> loadFromCSV(
            MLFileSourceConfig fileConfig,
            MLFeatureConfig featureConfig,
            CSVExampleFactory<T> exampleFactory,
            String primaryLabelColumn) throws Exception {

        List<Example<T>> examples = new ArrayList<>();
        List<String> featureColumns = featureConfig.getFeatureColumns();
        String[] featureNames = featureColumns.toArray(new String[0]);

        try (BufferedReader reader = new BufferedReader(new FileReader(fileConfig.getFilePath()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("Empty CSV file");

            String delimiter = fileConfig.getDelimiter();
            String[] headers = headerLine.split(delimiter);

            // Find column indices
            int[] featureIndices = findColumnIndices(headers, featureColumns);
            int labelIndex = primaryLabelColumn != null ? findColumnIndex(headers, primaryLabelColumn) : -1;

            // For regression, also find label column indices
            List<String> labelColumns = featureConfig.getLabelColumns();
            int[] labelIndices = findColumnIndices(headers, labelColumns);

            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(delimiter);
                double[] featureValues = extractFeatureValues(values, featureIndices);
                Example<T> example = exampleFactory.create(featureNames, featureValues, values, labelIndex);
                examples.add(example);
            }
        }

        return examples;
    }

    // ==================== Helper Methods ====================

    private double[] extractFeatureValues(ResultSet rs, List<String> featureColumns) throws SQLException {
        double[] values = new double[featureColumns.size()];
        for (int i = 0; i < featureColumns.size(); i++) {
            values[i] = rs.getDouble(featureColumns.get(i));
        }
        return values;
    }

    private double[] extractFeatureValues(String[] row, int[] featureIndices) {
        double[] values = new double[featureIndices.length];
        for (int i = 0; i < featureIndices.length; i++) {
            values[i] = Double.parseDouble(row[featureIndices[i]].trim());
        }
        return values;
    }

    private int[] findColumnIndices(String[] headers, List<String> columns) {
        int[] indices = new int[columns.size()];
        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim();
            for (int j = 0; j < columns.size(); j++) {
                if (header.equals(columns.get(j))) {
                    indices[j] = i;
                }
            }
        }
        return indices;
    }

    private int findColumnIndex(String[] headers, String column) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equals(column)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Column not found: " + column);
    }

    private Regressor createRegressorFromResultSet(List<String> labelColumns, ResultSet rs) throws SQLException {
        if (labelColumns.size() == 1) {
            String labelColumn = labelColumns.get(0);
            return new Regressor(labelColumn, rs.getDouble(labelColumn));
        } else {
            String[] names = new String[labelColumns.size()];
            double[] values = new double[labelColumns.size()];
            for (int i = 0; i < labelColumns.size(); i++) {
                names[i] = labelColumns.get(i);
                values[i] = rs.getDouble(labelColumns.get(i));
            }
            return new Regressor(names, values);
        }
    }

    private Regressor createRegressorFromArray(String[] labelNames, int[] labelIndices, String[] values) {
        if (labelNames.length == 1) {
            return new Regressor(labelNames[0], Double.parseDouble(values[labelIndices[0]].trim()));
        } else {
            double[] targetValues = new double[labelNames.length];
            for (int i = 0; i < labelNames.length; i++) {
                targetValues[i] = Double.parseDouble(values[labelIndices[i]].trim());
            }
            return new Regressor(labelNames, targetValues);
        }
    }

    private String buildQuery(MLTableSourceConfig tableConfig, MLFeatureConfig featureConfig) {
        StringBuilder sql = new StringBuilder("SELECT ");

        List<String> featureColumns = featureConfig.getFeatureColumns();
        for (int i = 0; i < featureColumns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(featureColumns.get(i));
        }

        List<String> labelColumns = featureConfig.getLabelColumns();
        for (String labelColumn : labelColumns) {
            sql.append(", ").append(labelColumn);
        }

        sql.append(" FROM ");
        if (tableConfig.getSchemaName() != null && !tableConfig.getSchemaName().isEmpty()) {
            sql.append(tableConfig.getSchemaName()).append(".");
        }
        sql.append(tableConfig.getTableName());

        return sql.toString();
    }

    // ==================== Functional Interfaces ====================

    @FunctionalInterface
    private interface DatabaseExampleFactory<T extends Output<T>> {
        Example<T> create(String[] featureNames, double[] featureValues, ResultSet rs, String labelColumn) throws SQLException;
    }

    @FunctionalInterface
    private interface CSVExampleFactory<T extends Output<T>> {
        Example<T> create(String[] featureNames, double[] featureValues, String[] values, int labelIndex);
    }
}
