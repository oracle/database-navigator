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
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/generalized-linear-model.html",
            MLTaskType.CLASSIFICATION
    ),

    SVM_CLASSIFICATION(
            "Support Vector Machine",
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/support-vector-machine.html",
            MLTaskType.CLASSIFICATION
    ),

    DECISION_TREE(
            "Decision Tree",
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/decision-tree.html",
            MLTaskType.CLASSIFICATION
    ),

    NAIVE_BAYES(
            "Naive Bayes",
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/naive-bayes.html",
            MLTaskType.CLASSIFICATION
    ),

    RANDOM_FOREST(
            "Random Forest",
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/random-forest.html",
            MLTaskType.CLASSIFICATION
    ),

    NEURAL_NETWORK_CLASSIFICATION(
            "Neural Network",
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/neural-network.html",
            MLTaskType.CLASSIFICATION
    ),

    XGBOOST_CLASSIFICATION(
            "XGBoost",
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/xgboost.html",
            MLTaskType.CLASSIFICATION
    ),

    // ========== REGRESSION TRAINERS ==========

    LINEAR_REGRESSION(
            "Linear Regression",
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/generalized-linear-model.html",
            MLTaskType.REGRESSION
    ),

    SVM_REGRESSION(
            "SVM Regression",
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/support-vector-machine.html",
            MLTaskType.REGRESSION
    ),

    NEURAL_NETWORK_REGRESSION(
            "Neural Network Regression",
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/neural-network.html",
            MLTaskType.REGRESSION
    ),

    XGBOOST_REGRESSION(
            "XGBoost Regression",
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/xgboost.html",
            MLTaskType.REGRESSION
    );

    private final String name;
    private final String docUrl;
    private final MLTaskType taskType;

    MLTrainerType(String name, String docUrl, MLTaskType taskType) {
        this.name = name;
        this.docUrl = docUrl;
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

    public static List<MLTrainerType> getClassificationTrainers() {
        return getTrainersForTask(MLTaskType.CLASSIFICATION);
    }

    public static List<MLTrainerType> getRegressionTrainers() {
        return getTrainersForTask(MLTaskType.REGRESSION);
    }
}
