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
            "Tree-based classification algorithm"
    ),

    NAIVE_BAYES(
            MLTaskType.CLASSIFICATION,
            "Naive Bayes",
            "ALGO_NAIVE_BAYES",
            "Probabilistic classifier based on Bayes theorem"
    ),

    RANDOM_FOREST(
            MLTaskType.CLASSIFICATION,
            "Random Forest",
            "ALGO_RANDOM_FOREST",
            "Ensemble of decision trees"
    ),

    SVM_CLASSIFICATION(
            MLTaskType.CLASSIFICATION,
            "Support Vector Machine",
            "ALGO_SUPPORT_VECTOR_MACHINES",
            "Support Vector Machine for classification"
    ),

    LOGISTIC_REGRESSION(
            MLTaskType.CLASSIFICATION,
            "Logistic Regression",
            "ALGO_GENERALIZED_LINEAR_MODEL",
            "Generalized Linear Model for classification"
    ),

    NEURAL_NETWORK_CLASSIFICATION(
            MLTaskType.CLASSIFICATION,
            "Neural Network",
            "ALGO_NEURAL_NETWORK",
            "Neural network for classification"
    ),

    // ==================== Regression Algorithms ====================

    LINEAR_REGRESSION(
            MLTaskType.REGRESSION,
            "Linear Regression",
            "ALGO_GENERALIZED_LINEAR_MODEL",
            "Generalized Linear Model for regression"
    ),

    SVM_REGRESSION(
            MLTaskType.REGRESSION,
            "SVM Regression",
            "ALGO_SUPPORT_VECTOR_MACHINES",
            "Support Vector Machine for regression"
    ),

    NEURAL_NETWORK_REGRESSION(
            MLTaskType.REGRESSION,
            "Neural Network Regression",
            "ALGO_NEURAL_NETWORK",
            "Neural network for regression"
    );

    // Note: Clustering, Feature Extraction, Anomaly Detection, and Association Rules
    // are not included in initial implementation as they require different workflows

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
