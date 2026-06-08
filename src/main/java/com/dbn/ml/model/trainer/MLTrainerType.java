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
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.ml.model.MLTaskType.CLASSIFICATION;
import static com.dbn.ml.model.MLTaskType.REGRESSION;
import static com.dbn.nls.NlsResources.txt;

/**
 * Supported ML trainer types for Oracle DBMS_DATA_MINING.
 */
@Getter
public enum MLTrainerType implements Presentable {

    // ========== CLASSIFICATION TRAINERS ==========

    LOGISTIC_REGRESSION(
            CLASSIFICATION,
            txt("app.machineLearning.const.MLTrainerType_LOGISTIC_REGRESSION"),
            txt("app.machineLearning.text.MLTrainerType_LOGISTIC_REGRESSION")
    ),

    SVM_CLASSIFICATION(
            CLASSIFICATION,
            txt("app.machineLearning.const.MLTrainerType_SVM_CLASSIFICATION"),
            txt("app.machineLearning.text.MLTrainerType_SVM_CLASSIFICATION")
    ),

    DECISION_TREE(
            CLASSIFICATION,
            txt("app.machineLearning.const.MLTrainerType_DECISION_TREE"),
            txt("app.machineLearning.text.MLTrainerType_DECISION_TREE")
    ),

    NAIVE_BAYES(
            CLASSIFICATION,
            txt("app.machineLearning.const.MLTrainerType_NAIVE_BAYES"),
            txt("app.machineLearning.text.MLTrainerType_NAIVE_BAYES")
    ),

    RANDOM_FOREST(
            CLASSIFICATION,
            txt("app.machineLearning.const.MLTrainerType_RANDOM_FOREST"),
            txt("app.machineLearning.text.MLTrainerType_RANDOM_FOREST")
    ),

    NEURAL_NETWORK_CLASSIFICATION(
            CLASSIFICATION,
            txt("app.machineLearning.const.MLTrainerType_NEURAL_NETWORK_CLASSIFICATION"),
            txt("app.machineLearning.text.MLTrainerType_NEURAL_NETWORK_CLASSIFICATION")
    ),

    XGBOOST_CLASSIFICATION(
            CLASSIFICATION,
            txt("app.machineLearning.const.MLTrainerType_XGBOOST_CLASSIFICATION"),
            txt("app.machineLearning.text.MLTrainerType_XGBOOST_CLASSIFICATION")
    ),

    // ========== REGRESSION TRAINERS ==========

    LINEAR_REGRESSION(
            REGRESSION,
            txt("app.machineLearning.const.MLTrainerType_LINEAR_REGRESSION"),
            txt("app.machineLearning.text.MLTrainerType_LINEAR_REGRESSION")
    ),

    SVM_REGRESSION(
            REGRESSION, txt("app.machineLearning.const.MLTrainerType_SVM_REGRESSION"),
            txt("app.machineLearning.text.MLTrainerType_SVM_REGRESSION")
    ),

    NEURAL_NETWORK_REGRESSION(
            REGRESSION,
            txt("app.machineLearning.const.MLTrainerType_NEURAL_NETWORK_REGRESSION"),
            txt("app.machineLearning.text.MLTrainerType_NEURAL_NETWORK_REGRESSION")
    ),

    XGBOOST_REGRESSION(
            REGRESSION,
            txt("app.machineLearning.const.MLTrainerType_XGBOOST_REGRESSION"),
            txt("app.machineLearning.text.MLTrainerType_XGBOOST_REGRESSION")
    );

    private final MLTaskType taskType;
    private final @Nls String name;
    private final @Nls String description;

    MLTrainerType(MLTaskType taskType, @Nls String name, @Nls String description) {
        this.name = name;
        this.description = description;
        this.taskType = taskType;
    }

    /**
     * Returns the algorithm name for the DBMS backend.
     */
    public @NonNls String getDBMSAlgorithmName() {
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
        return getTrainersForTask(CLASSIFICATION);
    }

    /**
     * Returns all regression trainers.
     */
    public static List<MLTrainerType> getRegressionTrainers() {
        return getTrainersForTask(REGRESSION);
    }
}
