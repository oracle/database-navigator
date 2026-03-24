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

package com.dbn.ml.model.trainer;

import com.dbn.common.ui.Presentable;
import com.dbn.ml.model.MLTaskType;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Supported ML trainer types for Oracle DBMS_DATA_MINING.
 */
@Getter
public enum MLTrainerType implements Presentable {

    // ========== CLASSIFICATION TRAINERS ==========

    LOGISTIC_REGRESSION(
            "Logistic Regression",
            "Generalized Linear Model that estimates the probability of class membership using a logistic function.",
            MLTaskType.CLASSIFICATION
    ),

    SVM_CLASSIFICATION(
            "Support Vector Machine",
            "Finds the optimal hyperplane to separate classes. Works well for high-dimensional data and binary or multiclass problems.",
            MLTaskType.CLASSIFICATION
    ),

    DECISION_TREE(
            "Decision Tree",
            "Builds a tree of binary splits on attribute values. Easy to interpret and visualize.",
            MLTaskType.CLASSIFICATION
    ),

    NAIVE_BAYES(
            "Naive Bayes",
            "Probabilistic classifier based on Bayes theorem, assuming independence between features. Fast and effective for text and categorical data.",
            MLTaskType.CLASSIFICATION
    ),

    RANDOM_FOREST(
            "Random Forest",
            "Ensemble of decision trees trained on random subsets of data and features. Provides variable importance ranking.",
            MLTaskType.CLASSIFICATION
    ),

    NEURAL_NETWORK_CLASSIFICATION(
            "Neural Network",
            "Multi-layer perceptron that learns non-linear patterns through backpropagation. Suitable for complex classification problems.",
            MLTaskType.CLASSIFICATION
    ),

    XGBOOST_CLASSIFICATION(
            "XGBoost",
            "Extreme Gradient Boosting — builds an ensemble of trees sequentially, each correcting errors of the previous. High accuracy on structured data.",
            MLTaskType.CLASSIFICATION
    ),

    // ========== REGRESSION TRAINERS ==========

    LINEAR_REGRESSION(
            "Linear Regression",
            "Generalized Linear Model that predicts a numeric target as a linear combination of input features.",
            MLTaskType.REGRESSION
    ),

    SVM_REGRESSION(
            "SVM Regression",
            "Support Vector Machine using epsilon-insensitive loss to predict numeric target values.",
            MLTaskType.REGRESSION
    ),

    NEURAL_NETWORK_REGRESSION(
            "Neural Network Regression",
            "Multi-layer perceptron trained to predict continuous numeric values from input features.",
            MLTaskType.REGRESSION
    ),

    XGBOOST_REGRESSION(
            "XGBoost Regression",
            "Extreme Gradient Boosting for numeric prediction. Combines multiple weak learners for high predictive accuracy.",
            MLTaskType.REGRESSION
    );

    private final String name;
    private final String description;
    private final MLTaskType taskType;

    MLTrainerType(String name, String description, MLTaskType taskType) {
        this.name = name;
        this.description = description;
        this.taskType = taskType;
    }

    /**
     * Returns the algorithm name for the DBMS backend.
     */
    public String getDBMSAlgorithmName() {
        return switch (this) {
            case LOGISTIC_REGRESSION -> "Logistic Regression";
            case SVM_CLASSIFICATION -> "Support Vector Machine";
            case DECISION_TREE -> "Decision Tree";
            case NAIVE_BAYES -> "Naive Bayes";
            case RANDOM_FOREST -> "Random Forest";
            case NEURAL_NETWORK_CLASSIFICATION -> "Neural Network";
            case XGBOOST_CLASSIFICATION -> "XGBoost";
            case LINEAR_REGRESSION -> "Linear Regression";
            case SVM_REGRESSION -> "SVM Regression";
            case NEURAL_NETWORK_REGRESSION -> "Neural Network Regression";
            case XGBOOST_REGRESSION -> "XGBoost Regression";
        };
    }

    /**
     * Returns all trainers for a specific task type.
     */
    public static List<MLTrainerType> getTrainersForTask(MLTaskType taskType) {
        return Arrays.stream(values())
                .filter(t -> t.getTaskType() == taskType)
                .collect(Collectors.toList());
    }

    /**
     * Returns all classification trainers.
     */
    public static List<MLTrainerType> getClassificationTrainers() {
        return getTrainersForTask(MLTaskType.CLASSIFICATION);
    }

    /**
     * Returns all regression trainers.
     */
    public static List<MLTrainerType> getRegressionTrainers() {
        return getTrainersForTask(MLTaskType.REGRESSION);
    }
}
