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
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMLInterface;
import com.dbn.ml.backend.model.MLTrainingContext;
import com.dbn.ml.model.MLTaskType;
import com.dbn.ml.model.source.MLFileSourceConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Manages data preparation for DBMS_DATA_MINING backend.
 * Handles staging table creation, CSV loading, and cleanup.
 *
 * @author Oracle
 */
@Slf4j
public class DBMSDataManager {

    /**
     * Creates a staging table and loads CSV data into it.
     *
     * @param context Training context
     * @return Table name (without schema prefix)
     */
    public String createAndLoadStagingTable(MLTrainingContext context) throws Exception {
        ConnectionHandler connection = context.getConnection();
        MLFileSourceConfig fileConfig = context.getSourceConfig().getFileSourceConfig();
        List<String> featureColumns = context.getFeatureConfig().getFeatureColumns();

        // Generate unique table name
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String tableName = "ML_STAGING_" + timestamp;
        String schemaName = getSchemaName(connection, context);

        // Read CSV and detect column types
        CSVParseResult parseResult = parseCSVFile(fileConfig, featureColumns,
                context.getFeatureConfig().getLabelColumns());

        // Create table with detected types
        createStagingTable(connection, schemaName, tableName, context, parseResult.numericFeatures);

        // Load CSV data
        int rowCount = loadCSVData(connection, tableName, context, parseResult);

        // Update context
        context.setStagingTableSchema(schemaName);
        context.setStagingTableName(tableName);
        context.setTrainingDataSize(rowCount);
        context.setShouldCleanupStagingTable(true);

        log.info("Created and loaded staging table {} with {} rows", tableName, rowCount);
        return tableName;
    }

    /**
     * Holds parsed CSV data and detected column types.
     */
    private static class CSVParseResult {
        List<String[]> dataRows = new ArrayList<>();
        int[] featureIndices;
        int[] labelIndices;
        boolean[] numericFeatures;
    }

    /**
     * Parses CSV file and auto-detects which feature columns are numeric vs categorical.
     */
    private CSVParseResult parseCSVFile(MLFileSourceConfig fileConfig,
                                         List<String> featureColumns,
                                         List<String> labelColumns) throws Exception {
        CSVParseResult result = new CSVParseResult();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileConfig.getFilePath()))) {
            // Parse header
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IllegalArgumentException("Empty CSV file");

            String[] headers = headerLine.split(fileConfig.getDelimiter());
            result.featureIndices = findColumnIndices(headers, featureColumns);
            result.labelIndices = findColumnIndices(headers, labelColumns);
            result.numericFeatures = new boolean[featureColumns.size()];

            // Read all data rows
            String line;
            while ((line = reader.readLine()) != null) {
                result.dataRows.add(line.split(fileConfig.getDelimiter()));
            }

            // Detect numeric vs categorical for each feature column
            for (int i = 0; i < result.featureIndices.length; i++) {
                result.numericFeatures[i] = isNumericColumn(result.dataRows, result.featureIndices[i]);
            }
        }

        return result;
    }

    /**
     * Checks if a column contains only numeric values.
     */
    private boolean isNumericColumn(List<String[]> dataRows, int columnIndex) {
        for (String[] row : dataRows) {
            if (columnIndex >= row.length) continue;
            String value = row[columnIndex].trim();
            if (value.isEmpty()) continue;
            try {
                Double.parseDouble(value);
            } catch (NumberFormatException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Drops the staging table if it exists.
     */
    public void dropStagingTable(MLTrainingContext context) throws SQLException {
        String schemaName = context.getStagingTableSchema();
        String tableName = context.getStagingTableName();

        if (tableName == null || tableName.isEmpty()) {
            return;
        }

        ConnectionHandler connection = context.getConnection();

        DatabaseInterfaceInvoker.execute(Priority.LOW,
                "Cleanup",
                "Dropping staging table",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseMLInterface mlInterface = connection.getInterfaces().getMLInterface();
                    if (mlInterface.tableExists(conn, schemaName, tableName)) {
                        mlInterface.dropStagingTable(conn, schemaName, tableName);
                        log.info("Dropped staging table: {}.{}", schemaName, tableName);
                    }
                });
    }

    // ==================== Private Methods ====================

    private String getSchemaName(ConnectionHandler connection, MLTrainingContext context) {
        String schemaName = context.getStagingTableSchema();
        return (schemaName == null || schemaName.isEmpty())
                ? connection.getUserName()
                : schemaName;
    }

    private void createStagingTable(ConnectionHandler connection, String schemaName,
                                    String tableName, MLTrainingContext context,
                                    boolean[] numericFeatures) throws SQLException {
        List<String> featureColumns = context.getFeatureConfig().getFeatureColumns();
        List<String> labelColumns = context.getFeatureConfig().getLabelColumns();
        boolean isClassification = context.getTaskType() == MLTaskType.CLASSIFICATION;

        // Build column definitions with auto-detected types
        StringBuilder columnDefs = new StringBuilder();
        for (int i = 0; i < featureColumns.size(); i++) {
            if (i > 0) columnDefs.append(", ");
            columnDefs.append(featureColumns.get(i));
            columnDefs.append(numericFeatures[i] ? " NUMBER" : " VARCHAR2(255)");
        }
        for (String labelColumn : labelColumns) {
            columnDefs.append(", ").append(labelColumn);
            columnDefs.append(isClassification ? " VARCHAR2(100)" : " NUMBER");
        }

        // Execute create table
        String columnDefsStr = columnDefs.toString();
        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Creating Table",
                "Creating staging table for CSV data",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseMLInterface mlInterface = connection.getInterfaces().getMLInterface();
                    mlInterface.createStagingTable(conn, schemaName, tableName, columnDefsStr);
                });

        log.info("Created staging table: {}.{}", schemaName, tableName);
    }

    private int loadCSVData(ConnectionHandler connection, String tableName,
                            MLTrainingContext context, CSVParseResult parseResult) throws Exception {
        List<String> featureColumns = context.getFeatureConfig().getFeatureColumns();
        List<String> labelColumns = context.getFeatureConfig().getLabelColumns();
        boolean isClassification = context.getTaskType() == MLTaskType.CLASSIFICATION;

        // Build INSERT statement
        String insertSql = buildInsertSql(tableName, featureColumns, labelColumns);

        return DatabaseInterfaceInvoker.load(Priority.HIGH,
                "Loading Data",
                "Loading CSV data into staging table",
                connection.getProject(),
                connection.getConnectionId(),
                conn -> {
                    int rowCount = 0;
                    int batchSize = 0;

                    try (PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                        for (String[] values : parseResult.dataRows) {
                            bindValues(stmt, values, parseResult.featureIndices, parseResult.labelIndices,
                                    parseResult.numericFeatures, isClassification);
                            stmt.addBatch();
                            batchSize++;
                            rowCount++;

                            if (batchSize >= 1000) {
                                stmt.executeBatch();
                                batchSize = 0;
                            }
                        }

                        if (batchSize > 0) {
                            stmt.executeBatch();
                        }
                    }

                    return rowCount;
                });
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

    private String buildInsertSql(String tableName, List<String> featureColumns, List<String> labelColumns) {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");

        for (int i = 0; i < featureColumns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(featureColumns.get(i));
        }
        for (String label : labelColumns) {
            sql.append(", ").append(label);
        }

        sql.append(") VALUES (");
        int totalColumns = featureColumns.size() + labelColumns.size();
        for (int i = 0; i < totalColumns; i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");

        return sql.toString();
    }

    private void bindValues(PreparedStatement stmt, String[] values,
                           int[] featureIndices, int[] labelIndices,
                           boolean[] numericFeatures, boolean isClassification) throws SQLException {
        int paramIndex = 1;

        // Bind feature values based on detected types
        for (int i = 0; i < featureIndices.length; i++) {
            int idx = featureIndices[i];
            String value = values[idx].trim();
            if (numericFeatures[i]) {
                stmt.setDouble(paramIndex++, value.isEmpty() ? 0.0 : Double.parseDouble(value));
            } else {
                stmt.setString(paramIndex++, value);
            }
        }

        // Bind label values
        for (int idx : labelIndices) {
            String value = values[idx].trim();
            if (isClassification) {
                stmt.setString(paramIndex++, value);
            } else {
                stmt.setDouble(paramIndex++, Double.parseDouble(value));
            }
        }
    }
}
