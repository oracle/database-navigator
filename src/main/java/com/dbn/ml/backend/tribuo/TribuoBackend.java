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
import com.dbn.ml.backend.MLBackend;
import com.dbn.ml.backend.MLBackendType;
import com.dbn.ml.backend.model.*;
import com.dbn.ml.model.MLTaskType;
import com.dbn.ml.model.trainer.MLTrainerConfig;
import com.dbn.ml.model.trainer.MLTrainerType;
import org.tribuo.Example;
import org.tribuo.Model;
import org.tribuo.MutableDataset;
import org.tribuo.Trainer;
import org.tribuo.classification.Label;
import org.tribuo.classification.LabelFactory;
import org.tribuo.classification.evaluation.LabelEvaluation;
import org.tribuo.classification.evaluation.LabelEvaluator;
import org.tribuo.datasource.ListDataSource;
import org.tribuo.evaluation.TrainTestSplitter;
import org.tribuo.impl.ArrayExample;
import org.tribuo.provenance.SimpleDataSourceProvenance;
import org.tribuo.regression.Regressor;
import org.tribuo.regression.RegressionFactory;
import org.tribuo.regression.evaluation.RegressionEvaluation;
import org.tribuo.regression.evaluation.RegressionEvaluator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tribuo implementation of MLBackend.
 * Trains models client-side using the Tribuo ML library.
 *
 * @author ayoub allai
 */
public class TribuoBackend implements MLBackend {

    private final TribuoDataLoader dataLoader = new TribuoDataLoader();

    @Override
    public MLBackendType getBackendType() {
        return MLBackendType.TRIBUO;
    }

    @Override
    public MLModelHandle train(MLTrainingContext context) throws Exception {
        MLTaskType taskType = context.getTaskType();

        return switch (taskType) {
            case CLASSIFICATION -> trainClassification(context);
            case REGRESSION -> trainRegression(context);
        };
    }

    /**
     * Train a classification model.
     */
    private TribuoModelHandle trainClassification(MLTrainingContext context) throws Exception {
        // Load classification data
        List<Example<Label>> examples = dataLoader.loadClassificationData(
                context.getSourceConfig(),
                context.getFeatureConfig(),
                context.getConnection()
        );

        // Create Tribuo data source
        LabelFactory labelFactory = new LabelFactory();
        String sourceName = getSourceName(context);
        SimpleDataSourceProvenance provenance = new SimpleDataSourceProvenance(
                "ML Training Data from " + sourceName,
                labelFactory
        );
        ListDataSource<Label> dataSource = new ListDataSource<>(examples, labelFactory, provenance);

        // Split data into training and testing sets
        MLTrainerConfig trainerConfig = context.getTrainerConfig();
        long seed = trainerConfig.isUseFixedSeed() ? trainerConfig.getRandomSeed() : System.currentTimeMillis();
        TrainTestSplitter<Label> splitter = new TrainTestSplitter<>(
                dataSource,
                trainerConfig.getTrainTestSplitRatio(),
                seed
        );

        MutableDataset<Label> trainingDataset = new MutableDataset<>(splitter.getTrain());
        MutableDataset<Label> testingDataset = new MutableDataset<>(splitter.getTest());

        // Update context with dataset sizes
        context.setTrainingDataSize(trainingDataset.size());
        context.setTestingDataSize(testingDataset.size());

        // Create and train the model
        MLTrainerType trainerType = trainerConfig.getTrainerType();
        Trainer<Label> trainer = trainerType.createClassificationTrainer();
        Model<Label> model = trainer.train(trainingDataset);

        // Create model handle
        List<String> featureNames = new ArrayList<>(context.getFeatureConfig().getFeatureColumns());
        List<String> labelNames = Arrays.asList(context.getFeatureConfig().getLabelColumn());

        return new TribuoModelHandle(
                model,
                MLTaskType.CLASSIFICATION,
                featureNames,
                labelNames,
                testingDataset,
                trainingDataset.size(),
                testingDataset.size()
        );
    }

    /**
     * Train a regression model.
     */
    private TribuoModelHandle trainRegression(MLTrainingContext context) throws Exception {
        // Load regression data
        List<Example<Regressor>> examples = dataLoader.loadRegressionData(
                context.getSourceConfig(),
                context.getFeatureConfig(),
                context.getConnection()
        );

        // Create Tribuo data source
        RegressionFactory regressionFactory = new RegressionFactory();
        String sourceName = getSourceName(context);
        SimpleDataSourceProvenance provenance = new SimpleDataSourceProvenance(
                "ML Training Data from " + sourceName,
                regressionFactory
        );
        ListDataSource<Regressor> dataSource = new ListDataSource<>(examples, regressionFactory, provenance);

        // Split data into training and testing sets
        MLTrainerConfig trainerConfig = context.getTrainerConfig();
        long seed = trainerConfig.isUseFixedSeed() ? trainerConfig.getRandomSeed() : System.currentTimeMillis();
        TrainTestSplitter<Regressor> splitter = new TrainTestSplitter<>(
                dataSource,
                trainerConfig.getTrainTestSplitRatio(),
                seed
        );

        MutableDataset<Regressor> trainingDataset = new MutableDataset<>(splitter.getTrain());
        MutableDataset<Regressor> testingDataset = new MutableDataset<>(splitter.getTest());

        // Update context with dataset sizes
        context.setTrainingDataSize(trainingDataset.size());
        context.setTestingDataSize(testingDataset.size());

        // Create and train the model
        MLTrainerType trainerType = trainerConfig.getTrainerType();
        Trainer<Regressor> trainer = trainerType.createRegressionTrainer();
        Model<Regressor> model = trainer.train(trainingDataset);

        // Create model handle
        List<String> featureNames = new ArrayList<>(context.getFeatureConfig().getFeatureColumns());
        List<String> labelNames = new ArrayList<>(context.getFeatureConfig().getLabelColumns());

        return new TribuoModelHandle(
                model,
                MLTaskType.REGRESSION,
                featureNames,
                labelNames,
                testingDataset,
                trainingDataset.size(),
                testingDataset.size()
        );
    }

    @Override
    public MLEvaluationResult evaluate(MLModelHandle modelHandle, MLTrainingContext context) throws Exception {
        TribuoModelHandle tribuoHandle = (TribuoModelHandle) modelHandle;
        MLTaskType taskType = modelHandle.getTaskType();

        if (taskType == MLTaskType.CLASSIFICATION) {
            Model<Label> model = (Model<Label>) tribuoHandle.getNativeModel();
            MutableDataset<Label> testDataset = (MutableDataset<Label>) tribuoHandle.getTestDataset();

            LabelEvaluator evaluator = new LabelEvaluator();
            LabelEvaluation evaluation = evaluator.evaluate(model, testDataset);

            return new TribuoEvaluationResult(evaluation, testDataset.size());
        } else {
            Model<Regressor> model = (Model<Regressor>) tribuoHandle.getNativeModel();
            MutableDataset<Regressor> testDataset = (MutableDataset<Regressor>) tribuoHandle.getTestDataset();

            RegressionEvaluator evaluator = new RegressionEvaluator();
            RegressionEvaluation evaluation = evaluator.evaluate(model, testDataset);

            return new TribuoEvaluationResult(evaluation, testDataset.size());
        }
    }

    @Override
    public MLPredictionResult predict(MLModelHandle modelHandle, Map<String, Double> featureValues) throws Exception {
        TribuoModelHandle tribuoHandle = (TribuoModelHandle) modelHandle;
        Model<?> model = (Model<?>) tribuoHandle.getNativeModel();
        MLTaskType taskType = modelHandle.getTaskType();

        // Convert featureValues map to Tribuo Example
        String[] featureNames = featureValues.keySet().toArray(new String[0]);
        double[] values = featureValues.values().stream().mapToDouble(Double::doubleValue).toArray();

        if (taskType == MLTaskType.CLASSIFICATION) {
            ArrayExample<Label> example = new ArrayExample<>(new Label("unknown"), featureNames, values);
            var prediction = ((Model<Label>) model).predict(example);
            return TribuoPredictionResult.fromClassification(prediction);
        } else {
            // For regression, create dummy Regressor output
            List<String> labelNames = tribuoHandle.getMetadata().getLabelNames();
            Regressor dummyOutput;
            if (labelNames.size() == 1) {
                dummyOutput = new Regressor(labelNames.get(0), 0.0);
            } else {
                String[] names = labelNames.toArray(new String[0]);
                double[] zeros = new double[names.length];
                dummyOutput = new Regressor(names, zeros);
            }

            ArrayExample<Regressor> example = new ArrayExample<>(dummyOutput, featureNames, values);
            var prediction = ((Model<Regressor>) model).predict(example);
            return TribuoPredictionResult.fromRegression(prediction);
        }
    }

    @Override
    public List<String> getAvailableAlgorithms(MLTaskType taskType) {
        return Arrays.stream(MLTrainerType.values())
                .filter(t -> t.getTaskType() == taskType)
                .map(MLTrainerType::getName)
                .collect(Collectors.toList());
    }

    @Override
    public void cleanup(MLTrainingContext context) throws Exception {
        // Tribuo is in-memory, nothing to cleanup
    }

    /**
     * Get a human-readable name for the data source.
     */
    private String getSourceName(MLTrainingContext context) {
        return switch (context.getSourceConfig().getSourceType()) {
            case DATABASE_TABLE -> {
                var tableConfig = context.getSourceConfig().getTableSourceConfig();
                yield tableConfig.getSchemaName() + "." + tableConfig.getTableName();
            }
            case FILE_SYSTEM -> {
                var fileConfig = context.getSourceConfig().getFileSourceConfig();
                String path = fileConfig.getFilePath();
                int lastSep = Math.max(path.lastIndexOf('/'), path.lastIndexOf('\\'));
                yield lastSep >= 0 ? path.substring(lastSep + 1) : path;
            }
            case OBJECT_STORAGE -> "Object Storage";
        };
    }
}
