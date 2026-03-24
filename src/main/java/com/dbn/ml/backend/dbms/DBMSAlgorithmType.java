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
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Oracle DBMS_DATA_MINING algorithm types.
 * Maps user-friendly names to Oracle algorithm constants.
 *
 * @author Oracle
 */
@Getter
public enum DBMSAlgorithmType {

    // ==================== Classification Algorithms ====================

    DECISION_TREE(
            MLTaskType.CLASSIFICATION,
            "Decision Tree",
            "ALGO_DECISION_TREE",
            "Builds a tree of binary splits on attribute values"
    ),

    NAIVE_BAYES(
            MLTaskType.CLASSIFICATION,
            "Naive Bayes",
            "ALGO_NAIVE_BAYES",
            "Probabilistic classifier based on Bayes theorem with feature independence assumption"
    ),

    RANDOM_FOREST(
            MLTaskType.CLASSIFICATION,
            "Random Forest",
            "ALGO_RANDOM_FOREST",
            "Ensemble of decision trees trained on random subsets of data and features"
    ),

    SVM_CLASSIFICATION(
            MLTaskType.CLASSIFICATION,
            "Support Vector Machine",
            "ALGO_SUPPORT_VECTOR_MACHINES",
            "Finds the optimal hyperplane to separate classes in high-dimensional space"
    ),

    LOGISTIC_REGRESSION(
            MLTaskType.CLASSIFICATION,
            "Logistic Regression",
            "ALGO_GENERALIZED_LINEAR_MODEL",
            "Generalized Linear Model that models class membership probability"
    ),

    NEURAL_NETWORK_CLASSIFICATION(
            MLTaskType.CLASSIFICATION,
            "Neural Network",
            "ALGO_NEURAL_NETWORK",
            "Multi-layer perceptron for non-linear classification via backpropagation"
    ),

    XGBOOST_CLASSIFICATION(
            MLTaskType.CLASSIFICATION,
            "XGBoost",
            "ALGO_XGBOOST",
            "Extreme Gradient Boosting — sequential ensemble of trees with high accuracy on structured data"
    ),

    // ==================== Regression Algorithms ====================

    LINEAR_REGRESSION(
            MLTaskType.REGRESSION,
            "Linear Regression",
            "ALGO_GENERALIZED_LINEAR_MODEL",
            "Generalized Linear Model for numeric target prediction"
    ),

    SVM_REGRESSION(
            MLTaskType.REGRESSION,
            "SVM Regression",
            "ALGO_SUPPORT_VECTOR_MACHINES",
            "Support Vector Machine using epsilon-insensitive loss for numeric prediction"
    ),

    NEURAL_NETWORK_REGRESSION(
            MLTaskType.REGRESSION,
            "Neural Network Regression",
            "ALGO_NEURAL_NETWORK",
            "Multi-layer perceptron trained to predict continuous numeric values"
    ),

    XGBOOST_REGRESSION(
            MLTaskType.REGRESSION,
            "XGBoost Regression",
            "ALGO_XGBOOST",
            "Extreme Gradient Boosting for numeric prediction with high predictive accuracy"
    );

    private final MLTaskType taskType;
    private final String displayName;
    private final String oracleAlgorithmName;
    private final String description;

    DBMSAlgorithmType(MLTaskType taskType, String displayName, String oracleAlgorithmName, String description) {
        this.taskType = taskType;
        this.displayName = displayName;
        this.oracleAlgorithmName = oracleAlgorithmName;
        this.description = description;
    }

    /**
     * Returns algorithms for a specific task type.
     */
    public static List<DBMSAlgorithmType> getAlgorithmsForTask(MLTaskType taskType) {
        return Arrays.stream(values())
                .filter(alg -> alg.getTaskType() == taskType)
                .collect(Collectors.toList());
    }

    /**
     * Returns the Oracle mining function name for this task type.
     */
    public static String getMiningFunction(MLTaskType taskType) {
        return switch (taskType) {
            case CLASSIFICATION -> "CLASSIFICATION";
            case REGRESSION -> "REGRESSION";
        };
    }

    /**
     * Finds algorithm by display name.
     */
    public static DBMSAlgorithmType fromDisplayName(String displayName) {
        for (DBMSAlgorithmType type : values()) {
            if (type.getDisplayName().equalsIgnoreCase(displayName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown DBMS algorithm: " + displayName);
    }
}
