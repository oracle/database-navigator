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
import com.dbn.ml.model.trainer.MLTrainerType;
import lombok.Getter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.ml.model.MLTaskType.CLASSIFICATION;
import static com.dbn.ml.model.MLTaskType.REGRESSION;
import static com.dbn.nls.NlsResources.txt;

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
            "ALGO_DECISION_TREE",
            CLASSIFICATION,
            txt("app.machineLearning.const.DBMSAlgorithmType_DECISION_TREE"),
            txt("app.machineLearning.text.DBMSAlgorithmType_DECISION_TREE")
    ),

    NAIVE_BAYES(
            "ALGO_NAIVE_BAYES",
            CLASSIFICATION,
            txt("app.machineLearning.const.DBMSAlgorithmType_NAIVE_BAYES"),
            txt("app.machineLearning.text.DBMSAlgorithmType_NAIVE_BAYES")
    ),

    RANDOM_FOREST(
            "ALGO_RANDOM_FOREST",
            CLASSIFICATION,
            txt("app.machineLearning.const.DBMSAlgorithmType_RANDOM_FOREST"),
            txt("app.machineLearning.text.DBMSAlgorithmType_RANDOM_FOREST")
    ),

    SVM_CLASSIFICATION(
            "ALGO_SUPPORT_VECTOR_MACHINES",
            CLASSIFICATION,
            txt("app.machineLearning.const.DBMSAlgorithmType_SVM_CLASSIFICATION"),
            txt("app.machineLearning.text.DBMSAlgorithmType_SVM_CLASSIFICATION")
    ),

    LOGISTIC_REGRESSION(
            "ALGO_GENERALIZED_LINEAR_MODEL",
            CLASSIFICATION,
            txt("app.machineLearning.const.DBMSAlgorithmType_LOGISTIC_REGRESSION"),
            txt("app.machineLearning.text.DBMSAlgorithmType_LOGISTIC_REGRESSION")
    ),

    NEURAL_NETWORK_CLASSIFICATION(
            "ALGO_NEURAL_NETWORK",
            CLASSIFICATION,
            txt("app.machineLearning.const.DBMSAlgorithmType_NEURAL_NETWORK_CLASSIFICATION"),
            txt("app.machineLearning.text.DBMSAlgorithmType_NEURAL_NETWORK_CLASSIFICATION")
    ),

    XGBOOST_CLASSIFICATION(
            "ALGO_XGBOOST",
            CLASSIFICATION,
            txt("app.machineLearning.const.DBMSAlgorithmType_XGBOOST_CLASSIFICATION"),
            txt("app.machineLearning.text.DBMSAlgorithmType_XGBOOST_CLASSIFICATION")
    ),

    // ==================== Regression Algorithms ====================

    LINEAR_REGRESSION(
            "ALGO_GENERALIZED_LINEAR_MODEL",
            REGRESSION,
            txt("app.machineLearning.const.DBMSAlgorithmType_LINEAR_REGRESSION"),
            txt("app.machineLearning.text.DBMSAlgorithmType_LINEAR_REGRESSION")
    ),

    SVM_REGRESSION(
            "ALGO_SUPPORT_VECTOR_MACHINES",
            REGRESSION,
            txt("app.machineLearning.const.DBMSAlgorithmType_SVM_REGRESSION"),
            txt("app.machineLearning.text.DBMSAlgorithmType_SVM_REGRESSION")
    ),

    NEURAL_NETWORK_REGRESSION(
            "ALGO_NEURAL_NETWORK",
            REGRESSION,
            txt("app.machineLearning.const.DBMSAlgorithmType_NEURAL_NETWORK_REGRESSION"),
            txt("app.machineLearning.text.DBMSAlgorithmType_NEURAL_NETWORK_REGRESSION")
    ),

    XGBOOST_REGRESSION(
            "ALGO_XGBOOST",
            REGRESSION,
            txt("app.machineLearning.const.DBMSAlgorithmType_XGBOOST_REGRESSION"),
            txt("app.machineLearning.text.DBMSAlgorithmType_XGBOOST_REGRESSION")
    );

    private final MLTaskType taskType;
    private final @NonNls String id;
    private final @Nls String displayName;
    private final @Nls String description;

    DBMSAlgorithmType(@NonNls String id, MLTaskType taskType, @Nls String displayName, @Nls String description) {
        this.id = id;
        this.taskType = taskType;
        this.displayName = displayName;
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
    public static @NonNls String getMiningFunction(MLTaskType taskType) {
        return switch (taskType) {
            case CLASSIFICATION -> "CLASSIFICATION";
            case REGRESSION -> "REGRESSION";
        };
    }

    public static DBMSAlgorithmType fromTrainerType(MLTrainerType trainerType) {
        return switch (trainerType) {
            case LOGISTIC_REGRESSION -> LOGISTIC_REGRESSION;
            case SVM_CLASSIFICATION -> SVM_CLASSIFICATION;
            case DECISION_TREE -> DECISION_TREE;
            case NAIVE_BAYES -> NAIVE_BAYES;
            case RANDOM_FOREST -> RANDOM_FOREST;
            case NEURAL_NETWORK_CLASSIFICATION -> NEURAL_NETWORK_CLASSIFICATION;
            case XGBOOST_CLASSIFICATION -> XGBOOST_CLASSIFICATION;
            case LINEAR_REGRESSION -> LINEAR_REGRESSION;
            case SVM_REGRESSION -> SVM_REGRESSION;
            case NEURAL_NETWORK_REGRESSION -> NEURAL_NETWORK_REGRESSION;
            case XGBOOST_REGRESSION -> XGBOOST_REGRESSION;
        };
    }
}
