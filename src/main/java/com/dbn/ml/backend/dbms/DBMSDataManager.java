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
import com.dbn.database.interfaces.DatabaseMachineLearningInterface;
import com.dbn.ml.backend.model.MLTrainingContext;
import com.dbn.ml.model.MLTaskType;
import com.dbn.ml.model.source.MLFileSourceConfig;
import com.dbn.ml.util.MLCSVParser;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Set;

import static com.dbn.nls.NlsResources.txt;

/**
 * Manages data preparation for DBMS_DATA_MINING backend.
 * Handles staging table creation, CSV loading, and cleanup.
 *
 * @author ayoub allali
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
        String tableName = MLObjectNames.stagingTable(MLObjectNames.timestamp());
        String schemaName = getSchemaName(connection, context);

        // Inspect the complete file before creating database objects.
        List<String> partitionColumns = context.getTrainerConfig().getPartitionColumns();
        MLCSVParser.Profile profile = profile(fileConfig);
        if (context.getTaskType() == MLTaskType.REGRESSION) {
            for (String labelColumn : context.getFeatureConfig().getLabelColumns()) {
                if (!profile.getNumericColumns().contains(labelColumn)) {
                    throw new IllegalArgumentException(txt(
                            "msg.machineLearning.exception.CsvRegressionTargetNotNumeric",
                            labelColumn));
                }
            }
        }
        CSVColumnMapping columnMapping = createColumnMapping(
                profile,
                featureColumns,
                context.getFeatureConfig().getLabelColumns(),
                partitionColumns);

        // Register the table before creation so failure cleanup can remove partial work.
        context.setStagingTableSchema(schemaName);
        context.setStagingTableName(tableName);
        context.setShouldCleanupStagingTable(true);

        // Create table with detected types
        createStagingTable(
                connection,
                schemaName,
                tableName,
                context,
                columnMapping.numericFeatures,
                columnMapping.numericPartitions);

        // Stream CSV data into the staging table.
        int rowCount = loadCSVData(connection, tableName, context, columnMapping, fileConfig);
        context.setTrainingDataSize(rowCount);

        log.info("Created and loaded staging table {} with {} rows", tableName, rowCount);
        return tableName;
    }

    /**
     * Maps selected ML columns to their positions and detected CSV types.
     */
    private static class CSVColumnMapping {
        private int columnCount;
        private int[] featureIndices;
        private int[] labelIndices;
        private int[] partitionIndices;
        private boolean[] numericFeatures;
        private boolean[] numericPartitions;
    }

    /**
     * Profiles the complete CSV so type and shape errors are found before creating the staging table.
     */
    private MLCSVParser.Profile profile(MLFileSourceConfig fileConfig) throws Exception {
        Path path = Path.of(fileConfig.getFilePath());
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return MLCSVParser.profile(reader, fileConfig.getDelimiter(), fileConfig.isHasHeader(), 0);
        }
    }

    private CSVColumnMapping createColumnMapping(
            MLCSVParser.Profile profile,
            List<String> featureColumns,
            List<String> labelColumns,
            List<String> partitionColumns) {
        CSVColumnMapping result = new CSVColumnMapping();
        List<String> columns = profile.getColumns();
        Set<String> numericColumns = profile.getNumericColumns();
        result.columnCount = columns.size();
        result.featureIndices = findColumnIndices(columns, featureColumns);
        result.labelIndices = findColumnIndices(columns, labelColumns);
        result.partitionIndices = findColumnIndices(columns, partitionColumns);
        result.numericFeatures = numericFlags(featureColumns, numericColumns);
        result.numericPartitions = numericFlags(partitionColumns, numericColumns);
        return result;
    }

    private boolean[] numericFlags(List<String> columns, Set<String> numericColumns) {
        boolean[] flags = new boolean[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            flags[i] = numericColumns.contains(columns.get(i));
        }
        return flags;
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
                txt("prc.machineLearning.title.Cleanup"),
                txt("prc.machineLearning.text.DroppingStagingTable"),
                connection.getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();
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
                                    boolean[] numericFeatures, boolean[] numericPartitions) throws SQLException {
        List<String> featureColumns = context.getFeatureConfig().getFeatureColumns();
        List<String> labelColumns = context.getFeatureConfig().getLabelColumns();
        List<String> partitionColumns = context.getTrainerConfig().getPartitionColumns();
        boolean isClassification = context.getTaskType() == MLTaskType.CLASSIFICATION;

        // Build column definitions with auto-detected types
        StringBuilder columnDefs = new StringBuilder();
        for (int i = 0; i < featureColumns.size(); i++) {
            if (i > 0) columnDefs.append(", ");
            columnDefs.append(featureColumns.get(i));
            columnDefs.append(numericFeatures[i] ? " NUMBER" : " VARCHAR2(" + MLCSVParser.MAX_TEXT_LENGTH + ")");
        }
        for (String labelColumn : labelColumns) {
            columnDefs.append(", ").append(labelColumn);
            columnDefs.append(isClassification ? " VARCHAR2(" + MLCSVParser.MAX_TEXT_LENGTH + ")" : " NUMBER");
        }
        // Partition columns must be present in the input table for ODMS_PARTITION_COLUMNS
        for (int i = 0; i < partitionColumns.size(); i++) {
            columnDefs.append(", ").append(partitionColumns.get(i));
            columnDefs.append(numericPartitions[i] ? " NUMBER" : " VARCHAR2(" + MLCSVParser.MAX_TEXT_LENGTH + ")");
        }

        // Execute create table
        String columnDefsStr = columnDefs.toString();
        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                txt("prc.machineLearning.title.CreatingTable"),
                txt("prc.machineLearning.text.CreatingCsvStagingTable"),
                connection.getProject(),
                connection.getConnectionId(),
                conn -> {
                    DatabaseMachineLearningInterface mlInterface = connection.getInterfaces().getMachineLearningInterface();
                    mlInterface.createStagingTable(conn, schemaName, tableName, columnDefsStr);
                });

        log.info("Created staging table: {}.{}", schemaName, tableName);
    }

    private int loadCSVData(
            ConnectionHandler connection,
            String tableName,
            MLTrainingContext context,
            CSVColumnMapping columnMapping,
            MLFileSourceConfig fileConfig) throws SQLException {
        List<String> featureColumns = context.getFeatureConfig().getFeatureColumns();
        List<String> labelColumns = context.getFeatureConfig().getLabelColumns();
        List<String> partitionColumns = context.getTrainerConfig().getPartitionColumns();
        boolean isClassification = context.getTaskType() == MLTaskType.CLASSIFICATION;

        // Build INSERT statement
        String insertSql = buildInsertSql(tableName, featureColumns, labelColumns, partitionColumns);

        return DatabaseInterfaceInvoker.load(Priority.HIGH,
                txt("prc.machineLearning.title.LoadingData"),
                txt("prc.machineLearning.text.LoadingCsvData"),
                connection.getProject(),
                connection.getConnectionId(),
                conn -> {
                    conn.setAutoCommit(false);
                    int rowCount = 0;
                    int batchSize = 0;
                    long csvRowNumber = fileConfig.isHasHeader() ? 2 : 1;

                    try (Reader source = Files.newBufferedReader(Path.of(fileConfig.getFilePath()), StandardCharsets.UTF_8);
                         CSVReader reader = MLCSVParser.createReader(source, fileConfig.getDelimiter());
                         PreparedStatement stmt = conn.prepareStatement(insertSql)) {
                        if (fileConfig.isHasHeader()) {
                            reader.readNext();
                        }

                        String[] values;
                        while ((values = reader.readNext()) != null) {
                            if (MLCSVParser.isEmpty(values)) {
                                csvRowNumber++;
                                continue;
                            }

                            MLCSVParser.validateRow(values, columnMapping.columnCount, csvRowNumber++);
                            bindValues(stmt, values, columnMapping.featureIndices, columnMapping.labelIndices,
                                    columnMapping.partitionIndices, columnMapping.numericFeatures,
                                    columnMapping.numericPartitions, isClassification);
                            stmt.addBatch();
                            batchSize++;
                            rowCount++;

                            if (batchSize >= 1000) {
                                stmt.executeBatch();
                                conn.commit();
                                batchSize = 0;
                            }
                        }

                        if (batchSize > 0) {
                            stmt.executeBatch();
                            conn.commit();
                        }
                    } catch (IOException | CsvValidationException e) {
                        throw new SQLException(e.getMessage(), e);
                    }

                    return rowCount;
                });
    }

    private int[] findColumnIndices(List<String> headers, List<String> columns) {
        int[] indices = new int[columns.size()];
        for (int i = 0; i < columns.size(); i++) {
            String column = columns.get(i);
            int index = headers.indexOf(column);
            if (index < 0) {
                throw new IllegalArgumentException(txt("msg.machineLearning.exception.CsvColumnMissing", column));
            }
            indices[i] = index;
        }
        return indices;
    }

    private String buildInsertSql(String tableName, List<String> featureColumns,
                                   List<String> labelColumns, List<String> partitionColumns) {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tableName).append(" (");

        for (int i = 0; i < featureColumns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(featureColumns.get(i));
        }
        for (String label : labelColumns) {
            sql.append(", ").append(label);
        }
        for (String partition : partitionColumns) {
            sql.append(", ").append(partition);
        }

        sql.append(") VALUES (");
        int totalColumns = featureColumns.size() + labelColumns.size() + partitionColumns.size();
        for (int i = 0; i < totalColumns; i++) {
            if (i > 0) sql.append(", ");
            sql.append("?");
        }
        sql.append(")");

        return sql.toString();
    }

    private void bindValues(PreparedStatement stmt, String[] values,
                           int[] featureIndices, int[] labelIndices, int[] partitionIndices,
                           boolean[] numericFeatures, boolean[] numericPartitions,
                           boolean isClassification) throws SQLException {
        int paramIndex = 1;

        // Bind feature values based on detected types
        for (int i = 0; i < featureIndices.length; i++) {
            int idx = featureIndices[i];
            String value = values[idx].trim();
            if (numericFeatures[i]) {
                setNumber(stmt, paramIndex++, value);
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
                setNumber(stmt, paramIndex++, value);
            }
        }

        // Bind partition column values
        for (int i = 0; i < partitionIndices.length; i++) {
            int idx = partitionIndices[i];
            String value = values[idx].trim();
            if (numericPartitions[i]) {
                setNumber(stmt, paramIndex++, value);
            } else {
                stmt.setString(paramIndex++, value);
            }
        }
    }

    private void setNumber(PreparedStatement statement, int index, String value) throws SQLException {
        if (value.isEmpty()) {
            statement.setNull(index, Types.NUMERIC);
        } else {
            statement.setBigDecimal(index, new BigDecimal(value));
        }
    }
}
