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
import lombok.Getter;
import org.tribuo.Trainer;
import org.tribuo.classification.Label;
import org.tribuo.classification.libsvm.LibSVMClassificationTrainer;
import org.tribuo.classification.libsvm.SVMClassificationType;
import org.tribuo.classification.sgd.linear.LogisticRegressionTrainer;
import org.tribuo.common.libsvm.KernelType;
import org.tribuo.common.libsvm.SVMParameters;

import java.util.function.Supplier;

/**
 * Supported ML trainer types for classification.
 * Based on Confluence examples.
 * 
 * @see <a href="https://tribuo.org/learn/4.3/javadoc/org/tribuo/Trainer.html">Tribuo Trainer</a>
 */
@Getter
public enum MLTrainerType implements Presentable {
    
    /**
     * Logistic Regression trainer using SGD.
     * From Confluence Example 1 (Classification with CSV).
     */
    LOGISTIC_REGRESSION(
            "Logistic Regression",
            "Linear classifier using stochastic gradient descent. Fast training, good baseline.",
            LogisticRegressionTrainer::new
    ),
    
    /**
     * LibSVM Classification trainer with Linear kernel.
     * From Confluence Example 2 (SVM with Database).
     */
    SVM_LINEAR(
            "SVM (Linear Kernel)",
            "Support Vector Machine with linear kernel. Supports ONNX export.",
            () -> new LibSVMClassificationTrainer(new SVMParameters<>(
                    new SVMClassificationType(SVMClassificationType.SVMMode.C_SVC),
                    KernelType.LINEAR
            ))
    );

    private final String name;
    private final String description;
    private final Supplier<Trainer<Label>> trainerSupplier;

    MLTrainerType(String name, String description, Supplier<Trainer<Label>> trainerSupplier) {
        this.name = name;
        this.description = description;
        this.trainerSupplier = trainerSupplier;
    }

    public Trainer<Label> createTrainer() {
        return trainerSupplier.get();
    }
}
