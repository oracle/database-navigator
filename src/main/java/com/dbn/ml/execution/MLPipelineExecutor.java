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

package com.dbn.ml.execution;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.ml.model.MLRequest;
import com.dbn.ml.model.MLResult;
import com.dbn.ml.model.feature.MLFeatureConfig;
import com.dbn.ml.model.source.MLSourceConfig;
import com.dbn.ml.model.source.MLSourceType;
import com.dbn.ml.model.source.MLTableSourceConfig;
import com.dbn.ml.model.source.MLFileSourceConfig;
import com.dbn.ml.model.trainer.MLTrainerConfig;
import com.dbn.ml.model.trainer.MLTrainerType;
import org.tribuo.Example;
import org.tribuo.MutableDataset;
import org.tribuo.Trainer;
import org.tribuo.Model;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.evaluation.LabelEvaluation;
import org.tribuo.classification.evaluation.LabelEvaluator;
import org.tribuo.data.csv.CSVLoader;
import org.tribuo.datasource.ListDataSource;
import org.tribuo.evaluation.TrainTestSplitter;
import org.tribuo.impl.ArrayExample;
import org.tribuo.provenance.SimpleDataSourceProvenance;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Executes ML training pipeline.
 * Supports multiple data sources following VectorToolbox pattern.
 */
public class MLPipelineExecutor {

    public MLResult execute(MLRequest request, ConnectionHandler connectionHandler) throws Exception {
        MLSourceConfig sourceConfig = request.getSourceConfig();
        MLFeatureConfig featureConfig = request.getFeatureConfig();
        MLTrainerConfig trainerConfig = request.getTrainerConfig();
        MLTrainerType trainerType = trainerConfig.getTrainerType();

        MLResult result = new MLResult();
        result.setConnection(connectionHandler);
        result.setAlgorithmName(trainerType.getName());
        
        long startTime = System.currentTimeMillis();

        // Load data based on source type
        List<Example<Label>> examples = loadData(sourceConfig, featureConfig, connectionHandler);
        
        // Create Tribuo data source
        LabelFactory labelFactory = new LabelFactory();
        String sourceName = getSourceName(sourceConfig);
        SimpleDataSourceProvenance provenance = new SimpleDataSourceProvenance(
                "ML Training Data from " + sourceName,
                labelFactory
        );
        ListDataSource<Label> dataSource = new ListDataSource<>(examples, labelFactory, provenance);

        // Split data into training and testing sets
        long seed = trainerConfig.isUseFixedSeed() ? trainerConfig.getRandomSeed() : System.currentTimeMillis();
        TrainTestSplitter<Label> splitter = new TrainTestSplitter<>(
                dataSource,
                trainerConfig.getTrainTestSplitRatio(),
                seed
        );

        MutableDataset<Label> trainingDataset = new MutableDataset<>(splitter.getTrain());
        MutableDataset<Label> testingDataset = new MutableDataset<>(splitter.getTest());

        // Record dataset info
        result.setTrainingDataSize(trainingDataset.size());
        result.setTestingDataSize(testingDataset.size());
        result.setFeatureCount(trainingDataset.getFeatureMap().size());
        result.setClassCount(trainingDataset.getOutputInfo().size());
        
        // Store original column names for ONNX metadata
        result.setFeatureColumns(new ArrayList<>(featureConfig.getFeatureColumns()));
        result.setLabelColumn(featureConfig.getLabelColumn());

        // Create and train the model
        Trainer<Label> trainer = trainerType.createTrainer();
        Model<Label> model = trainer.train(trainingDataset);
        result.setModel(model);

        // Evaluate the model
        LabelEvaluator evaluator = new LabelEvaluator();
        LabelEvaluation evaluation = evaluator.evaluate(model, testingDataset);
        result.setEvaluation(evaluation);

        // Record training time
        result.setTrainingTimeMs(System.currentTimeMillis() - startTime);

        return result;
    }

    /**
     * Load data based on source type.
     * Follows Strategy pattern for different data sources.
     */
    private List<Example<Label>> loadData(
            MLSourceConfig sourceConfig,
            MLFeatureConfig featureConfig,
            ConnectionHandler connectionHandler) throws Exception {
        
        MLSourceType sourceType = sourceConfig.getSourceType();
        
        return switch (sourceType) {
            case DATABASE_TABLE -> loadDataFromDatabase(
                    sourceConfig.getTableSourceConfig(),
                    featureConfig,
                    connectionHandler
            );
            case FILE_SYSTEM -> loadDataFromCSV(
                    sourceConfig.getFileSourceConfig(),
                    featureConfig
            );
            case OBJECT_STORAGE -> throw new UnsupportedOperationException(
                    "Object Storage source not yet implemented"
            );
        };
    }

    private String getSourceName(MLSourceConfig sourceConfig) {
        MLSourceType sourceType = sourceConfig.getSourceType();
        
        return switch (sourceType) {
            case DATABASE_TABLE -> {
                MLTableSourceConfig tableConfig = sourceConfig.getTableSourceConfig();
                yield tableConfig.getSchemaName() + "." + tableConfig.getTableName();
            }
            case FILE_SYSTEM -> {
                MLFileSourceConfig fileConfig = sourceConfig.getFileSourceConfig();
                String path = fileConfig.getFilePath();
                int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
                yield lastSep >= 0 ? path.substring(lastSep + 1) : path;
            }
            case OBJECT_STORAGE -> "Object Storage";
        };
    }

    /**
     * Load data from database table.
     */
    private List<Example<Label>> loadDataFromDatabase(
            MLTableSourceConfig tableConfig,
            MLFeatureConfig featureConfig,
            ConnectionHandler connectionHandler) throws SQLException {
        
        List<Example<Label>> examples = new ArrayList<>();
        String sql = buildQuery(tableConfig, featureConfig);
        List<String> featureColumns = featureConfig.getFeatureColumns();
        String labelColumn = featureConfig.getLabelColumn();
        
        // Convert feature column names to array for Tribuo
        String[] featureNames = featureColumns.toArray(new String[0]);

        try (DBNConnection conn = connectionHandler.getMainConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                // Extract feature values
                double[] featureValues = new double[featureColumns.size()];
                for (int i = 0; i < featureColumns.size(); i++) {
                    featureValues[i] = rs.getDouble(featureColumns.get(i));
                }
                
                // Extract label value
                String labelValue = rs.getString(labelColumn);
                
                // Create Tribuo example
                Example<Label> example = new ArrayExample<>(
                        new Label(labelValue),
                        featureNames,
                        featureValues
                );
                examples.add(example);
            }
        }
        
        return examples;
    }

    /**
     * Load data from CSV file using Tribuo's CSVLoader.
     */
    private List<Example<Label>> loadDataFromCSV(
            MLFileSourceConfig fileConfig,
            MLFeatureConfig featureConfig) throws IOException {
        
        String filePath = fileConfig.getFilePath();
        String labelColumn = featureConfig.getLabelColumn();
        List<String> featureColumns = featureConfig.getFeatureColumns();
        
        // Build headers array: features + label
        String[] headers = new String[featureColumns.size() + 1];
        for (int i = 0; i < featureColumns.size(); i++) {
            headers[i] = featureColumns.get(i);
        }
        headers[featureColumns.size()] = labelColumn;
        
        // Use Tribuo's CSVLoader
        LabelFactory labelFactory = new LabelFactory();
        CSVLoader<Label> csvLoader = new CSVLoader<>(
                fileConfig.getDelimiter().charAt(0),
                labelFactory
        );
        
        Path path = Paths.get(filePath);
        
        // Load data source
        var dataSource = csvLoader.loadDataSource(path, labelColumn, headers);
        
        // Convert to list
        List<Example<Label>> examples = new ArrayList<>();
        for (Example<Label> example : dataSource) {
            examples.add(example);
        }
        
        return examples;
    }

    private String buildQuery(MLTableSourceConfig tableConfig, MLFeatureConfig featureConfig) {
        StringBuilder sql = new StringBuilder("SELECT ");
        
        // Add feature columns
        List<String> featureColumns = featureConfig.getFeatureColumns();
        for (int i = 0; i < featureColumns.size(); i++) {
            if (i > 0) sql.append(", ");
            sql.append(featureColumns.get(i));
        }
        
        // Add label column
        sql.append(", ").append(featureConfig.getLabelColumn());
        
        // From table
        sql.append(" FROM ");
        if (tableConfig.getSchemaName() != null && !tableConfig.getSchemaName().isEmpty()) {
            sql.append(tableConfig.getSchemaName()).append(".");
        }
        sql.append(tableConfig.getTableName());
        
        return sql.toString();
    }
}
