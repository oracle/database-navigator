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
import com.dbn.ml.backend.MLBackendType;
import com.dbn.ml.model.MLTaskType;
import lombok.Getter;
import org.tribuo.Output;
import org.tribuo.Trainer;
import org.tribuo.classification.Label;
import org.tribuo.classification.libsvm.LibSVMClassificationTrainer;
import org.tribuo.classification.libsvm.SVMClassificationType;
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer;
import org.tribuo.common.libsvm.KernelType;
import org.tribuo.common.libsvm.SVMParameters;
import org.tribuo.regression.Regressor;
import org.tribuo.regression.libsvm.LibSVMRegressionTrainer;
import org.tribuo.regression.libsvm.SVMRegressionType;
import org.tribuo.regression.sgd.linear.LinearSGDTrainer;
import org.tribuo.regression.sgd.objectives.SquaredLoss;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Supported ML trainer types for classification and regression.
 * Each trainer can support one or more backends (Tribuo, DBMS_DATA_MINING).
 *
 * @see <a href="https://tribuo.org/learn/4.3/javadoc/org/tribuo/Trainer.html">Tribuo Trainer</a>
 */
@Getter
public enum MLTrainerType implements Presentable {

    // ========== CLASSIFICATION TRAINERS ==========

    /**
     * Logistic Regression trainer (both backends).
     */
    LOGISTIC_REGRESSION(
            "Logistic Regression",
            "Linear classifier. Tribuo: SGD-based. DBMS: Generalized Linear Model.",
            MLTaskType.CLASSIFICATION,
            EnumSet.of(MLBackendType.TRIBUO, MLBackendType.DBMS_DATA_MINING),
            () -> new LogisticRegressionTrainer()
    ),

    /**
     * SVM Classification trainer (both backends).
     */
    SVM_CLASSIFICATION(
            "Support Vector Machine",
            "Support Vector Machine classifier with linear kernel.",
            MLTaskType.CLASSIFICATION,
            EnumSet.of(MLBackendType.TRIBUO, MLBackendType.DBMS_DATA_MINING),
            () -> new LibSVMClassificationTrainer(new SVMParameters<>(
                    new SVMClassificationType(SVMClassificationType.SVMMode.C_SVC),
                    KernelType.LINEAR
            ))
    ),

    /**
     * Decision Tree classifier (DBMS only).
     */
    DECISION_TREE(
            "Decision Tree",
            "Tree-based classification algorithm. DBMS_DATA_MINING only.",
            MLTaskType.CLASSIFICATION,
            EnumSet.of(MLBackendType.DBMS_DATA_MINING),
            null
    ),

    /**
     * Naive Bayes classifier (DBMS only).
     */
    NAIVE_BAYES(
            "Naive Bayes",
            "Probabilistic classifier based on Bayes theorem. DBMS_DATA_MINING only.",
            MLTaskType.CLASSIFICATION,
            EnumSet.of(MLBackendType.DBMS_DATA_MINING),
            null
    ),

    /**
     * Random Forest classifier (DBMS only).
     */
    RANDOM_FOREST(
            "Random Forest",
            "Ensemble of decision trees. DBMS_DATA_MINING only.",
            MLTaskType.CLASSIFICATION,
            EnumSet.of(MLBackendType.DBMS_DATA_MINING),
            null
    ),

    /**
     * Neural Network classifier (DBMS only).
     */
    NEURAL_NETWORK_CLASSIFICATION(
            "Neural Network",
            "Neural network for classification. DBMS_DATA_MINING only.",
            MLTaskType.CLASSIFICATION,
            EnumSet.of(MLBackendType.DBMS_DATA_MINING),
            null
    ),

    // ========== REGRESSION TRAINERS ==========

    /**
     * Linear Regression trainer (both backends).
     */
    LINEAR_REGRESSION(
            "Linear Regression",
            "Linear regression. Tribuo: SGD-based. DBMS: Generalized Linear Model.",
            MLTaskType.REGRESSION,
            EnumSet.of(MLBackendType.TRIBUO, MLBackendType.DBMS_DATA_MINING),
            () -> new LinearSGDTrainer(
                    new SquaredLoss(),
                    new org.tribuo.math.optimisers.AdaGrad(0.01),
                    10,
                    1000,
                    1,
                    12345L
            )
    ),

    /**
     * SVM Regression trainer (both backends).
     */
    SVM_REGRESSION(
            "SVM Regression",
            "Support Vector Machine regression with linear kernel.",
            MLTaskType.REGRESSION,
            EnumSet.of(MLBackendType.TRIBUO, MLBackendType.DBMS_DATA_MINING),
            () -> new LibSVMRegressionTrainer(new SVMParameters<>(
                    new SVMRegressionType(SVMRegressionType.SVMMode.EPSILON_SVR),
                    KernelType.LINEAR
            ))
    ),

    /**
     * Neural Network regression (DBMS only).
     */
    NEURAL_NETWORK_REGRESSION(
            "Neural Network Regression",
            "Neural network for regression. DBMS_DATA_MINING only.",
            MLTaskType.REGRESSION,
            EnumSet.of(MLBackendType.DBMS_DATA_MINING),
            null
    );

    private final String name;
    private final String description;
    private final MLTaskType taskType;
    private final Set<MLBackendType> supportedBackends;
    private final Supplier<? extends Trainer<?>> trainerSupplier;

    MLTrainerType(String name, String description, MLTaskType taskType,
                  Set<MLBackendType> supportedBackends, Supplier<? extends Trainer<?>> trainerSupplier) {
        this.name = name;
        this.description = description;
        this.taskType = taskType;
        this.supportedBackends = supportedBackends;
        this.trainerSupplier = trainerSupplier;
    }

    /**
     * Checks if this trainer supports a specific backend.
     */
    public boolean supportsBackend(MLBackendType backendType) {
        return supportedBackends.contains(backendType);
    }

    /**
     * Returns the algorithm name for the DBMS backend.
     * This maps to the display names in DBMSAlgorithmType.
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
     * Creates a new trainer instance for Tribuo backend.
     * Throws if this trainer doesn't support Tribuo backend.
     */
    @SuppressWarnings("unchecked")
    public <T extends Output<T>> Trainer<T> createTrainer() {
        if (trainerSupplier == null) {
            throw new UnsupportedOperationException(
                    name + " does not support Tribuo backend. Use DBMS_DATA_MINING backend instead.");
        }
        return (Trainer<T>) trainerSupplier.get();
    }

    /**
     * Creates a classification trainer. Throws if this is not a classification type.
     */
    public Trainer<Label> createClassificationTrainer() {
        if (taskType != MLTaskType.CLASSIFICATION) {
            throw new IllegalStateException(name + " is not a classification trainer");
        }
        return createTrainer();
    }

    /**
     * Creates a regression trainer. Throws if this is not a regression type.
     */
    public Trainer<Regressor> createRegressionTrainer() {
        if (taskType != MLTaskType.REGRESSION) {
            throw new IllegalStateException(name + " is not a regression trainer");
        }
        return createTrainer();
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
     * Returns all trainers for a specific backend and task type.
     */
    public static List<MLTrainerType> getTrainersForBackendAndTask(MLBackendType backendType, MLTaskType taskType) {
        return Arrays.stream(values())
                .filter(t -> t.getTaskType() == taskType)
                .filter(t -> t.supportsBackend(backendType))
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
