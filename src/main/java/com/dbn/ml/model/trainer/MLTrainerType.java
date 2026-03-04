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
            "Generalized Linear Model classifier.",
            MLTaskType.CLASSIFICATION
    ),

    SVM_CLASSIFICATION(
            "Support Vector Machine",
            "Support Vector Machine classifier with linear kernel.",
            MLTaskType.CLASSIFICATION
    ),

    DECISION_TREE(
            "Decision Tree",
            "Tree-based classification algorithm.",
            MLTaskType.CLASSIFICATION
    ),

    NAIVE_BAYES(
            "Naive Bayes",
            "Probabilistic classifier based on Bayes theorem.",
            MLTaskType.CLASSIFICATION
    ),

    RANDOM_FOREST(
            "Random Forest",
            "Ensemble of decision trees.",
            MLTaskType.CLASSIFICATION
    ),

    NEURAL_NETWORK_CLASSIFICATION(
            "Neural Network",
            "Neural network for classification.",
            MLTaskType.CLASSIFICATION
    ),

    // ========== REGRESSION TRAINERS ==========

    LINEAR_REGRESSION(
            "Linear Regression",
            "Generalized Linear Model for regression.",
            MLTaskType.REGRESSION
    ),

    SVM_REGRESSION(
            "SVM Regression",
            "Support Vector Machine regression with linear kernel.",
            MLTaskType.REGRESSION
    ),

    NEURAL_NETWORK_REGRESSION(
            "Neural Network Regression",
            "Neural network for regression.",
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
            case LINEAR_REGRESSION -> "Linear Regression";
            case SVM_REGRESSION -> "SVM Regression";
            case NEURAL_NETWORK_REGRESSION -> "Neural Network Regression";
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
