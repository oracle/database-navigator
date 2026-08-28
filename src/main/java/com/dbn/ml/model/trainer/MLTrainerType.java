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

import static com.dbn.nls.NlsResources.txt;

/**
 * Supported ML trainer types for Oracle DBMS_DATA_MINING.
 */
@Getter
public enum MLTrainerType implements Presentable {

    // ========== CLASSIFICATION TRAINERS ==========

    LOGISTIC_REGRESSION(
            txt("app.machineLearning.const.MLTrainerType_LOGISTIC_REGRESSION"),
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/generalized-linear-model.html",
            MLTaskType.CLASSIFICATION
    ),

    SVM_CLASSIFICATION(
            txt("app.machineLearning.const.MLTrainerType_SVM_CLASSIFICATION"),
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/support-vector-machine.html",
            MLTaskType.CLASSIFICATION
    ),

    DECISION_TREE(
            txt("app.machineLearning.const.MLTrainerType_DECISION_TREE"),
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/decision-tree.html",
            MLTaskType.CLASSIFICATION
    ),

    NAIVE_BAYES(
            txt("app.machineLearning.const.MLTrainerType_NAIVE_BAYES"),
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/naive-bayes.html",
            MLTaskType.CLASSIFICATION
    ),

    RANDOM_FOREST(
            txt("app.machineLearning.const.MLTrainerType_RANDOM_FOREST"),
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/random-forest.html",
            MLTaskType.CLASSIFICATION
    ),

    NEURAL_NETWORK_CLASSIFICATION(
            txt("app.machineLearning.const.MLTrainerType_NEURAL_NETWORK_CLASSIFICATION"),
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/neural-network.html",
            MLTaskType.CLASSIFICATION
    ),

    XGBOOST_CLASSIFICATION(
            txt("app.machineLearning.const.MLTrainerType_XGBOOST_CLASSIFICATION"),
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/xgboost.html",
            MLTaskType.CLASSIFICATION
    ),

    // ========== REGRESSION TRAINERS ==========

    LINEAR_REGRESSION(
            txt("app.machineLearning.const.MLTrainerType_LINEAR_REGRESSION"),
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/generalized-linear-model.html",
            MLTaskType.REGRESSION
    ),

    SVM_REGRESSION(
            txt("app.machineLearning.const.MLTrainerType_SVM_REGRESSION"),
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/support-vector-machine.html",
            MLTaskType.REGRESSION
    ),

    NEURAL_NETWORK_REGRESSION(
            txt("app.machineLearning.const.MLTrainerType_NEURAL_NETWORK_REGRESSION"),
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/neural-network.html",
            MLTaskType.REGRESSION
    ),

    XGBOOST_REGRESSION(
            txt("app.machineLearning.const.MLTrainerType_XGBOOST_REGRESSION"),
            "https://docs.oracle.com/en/database/oracle/machine-learning/oml4sql/23/dmcon/xgboost.html",
            MLTaskType.REGRESSION
    );

    private final @Nls String name;
    private final @NonNls String docUrl;
    private final MLTaskType taskType;

    MLTrainerType(@Nls String name, @NonNls String docUrl, MLTaskType taskType) {
        this.name = name;
        this.docUrl = docUrl;
        this.taskType = taskType;
    }

    /**
     * Returns all trainers for a specific task type.
     */
    public static List<MLTrainerType> getTrainersForTask(MLTaskType taskType) {
        return Arrays.stream(values())
                .filter(t -> t.getTaskType() == taskType)
                .collect(Collectors.toList());
    }

}
