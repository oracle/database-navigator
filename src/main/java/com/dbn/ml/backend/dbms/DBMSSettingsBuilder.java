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

import com.dbn.ml.model.MLTaskType;
import com.dbn.ml.model.trainer.MLTrainerConfig;

import java.util.HashMap;
import java.util.Map;

/**
 * Builds DBMS_DATA_MINING settings for algorithms.
 *
 * @author ayoub allali
 */
public class DBMSSettingsBuilder {

    /**
     * Builds settings map for DBMS_DATA_MINING.
     *
     * @param taskType Task type (classification or regression)
     * @param algorithmName Oracle algorithm name (e.g., ALGO_DECISION_TREE)
     * @param trainerConfig Trainer configuration
     * @return Map of setting name → setting value
     */
    public Map<String, String> buildSettings(
            MLTaskType taskType,
            String algorithmName,
            MLTrainerConfig trainerConfig) {

        Map<String, String> settings = new HashMap<>();

        // Set algorithm
        settings.put("ALGO_NAME", algorithmName);

        // Automatic data preparation
        settings.put("PREP_AUTO", "ON");

        // Add algorithm-specific settings
        switch (algorithmName) {
            case "ALGO_DECISION_TREE" -> addDecisionTreeSettings(settings, trainerConfig);
            case "ALGO_NAIVE_BAYES" -> addNaiveBayesSettings(settings);
            case "ALGO_RANDOM_FOREST" -> addRandomForestSettings(settings, trainerConfig);
            case "ALGO_SUPPORT_VECTOR_MACHINES" -> addSVMSettings(settings, taskType);
            case "ALGO_GENERALIZED_LINEAR_MODEL" -> addGLMSettings(settings, taskType);
            case "ALGO_NEURAL_NETWORK" -> addNeuralNetworkSettings(settings, taskType);
        }

        return settings;
    }

    private void addDecisionTreeSettings(Map<String, String> settings, MLTrainerConfig config) {
        // Decision tree specific settings
        settings.put("TREE_IMPURITY_METRIC", "TREE_IMPURITY_GINI");
        settings.put("TREE_TERM_MAX_DEPTH", "7");
        settings.put("TREE_TERM_MINPCT_NODE", "0.05");
        settings.put("TREE_TERM_MINPCT_SPLIT", "0.1");
    }

    private void addNaiveBayesSettings(Map<String, String> settings) {
        // Naive Bayes specific settings
        settings.put("NABS_PAIRWISE_THRESHOLD", "0");
    }

    private void addRandomForestSettings(Map<String, String> settings, MLTrainerConfig config) {
        // Random Forest specific settings
        settings.put("RFOR_NUM_TREES", "20");
        settings.put("TREE_IMPURITY_METRIC", "TREE_IMPURITY_GINI");
        settings.put("TREE_TERM_MAX_DEPTH", "7");
    }

    private void addSVMSettings(Map<String, String> settings, MLTaskType taskType) {
        // SVM settings
        settings.put("SVMS_KERNEL_FUNCTION", "SVMS_LINEAR");
        settings.put("SVMS_COMPLEXITY_FACTOR", "1");

        if (taskType == MLTaskType.REGRESSION) {
            settings.put("SVMS_EPSILON", "0.1");
        }
    }

    private void addGLMSettings(Map<String, String> settings, MLTaskType taskType) {
        // Generalized Linear Model settings
        if (taskType == MLTaskType.CLASSIFICATION) {
            settings.put("GLMS_SOLVER", "GLMS_SOLVER_CHOL");
            settings.put("GLMS_RIDGE_REGRESSION", "GLMS_RIDGE_REG_ENABLE");
            settings.put("GLMS_RIDGE_VALUE", "1");
        } else {
            // Regression
            settings.put("GLMS_SOLVER", "GLMS_SOLVER_CHOL");
        }
    }

    private void addNeuralNetworkSettings(Map<String, String> settings, MLTaskType taskType) {
        // Neural Network settings
        settings.put("NNET_NODES_PER_LAYER", "10,10");
        settings.put("NNET_ACTIVATIONS", "NNET_ACTIVATIONS_RELU");
        settings.put("NNET_REGULARIZER", "NNET_REGULARIZER_NONE");
        settings.put("NNET_TOLERANCE", "0.001");
    }
}
