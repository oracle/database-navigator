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

package com.dbn.ml.backend.model;

import com.dbn.ml.model.MLTaskType;

import java.util.Map;

/**
 * Backend-agnostic evaluation metrics for trained models.
 *
 * @author Oracle
 */
public interface MLEvaluationResult {

    /**
     * Returns the task type being evaluated.
     */
    MLTaskType getTaskType();

    // ==================== Classification Metrics ====================

    /**
     * Returns the classification accuracy (0.0 to 1.0).
     * Only applicable for classification tasks.
     */
    double getAccuracy();

    /**
     * Returns the macro-averaged precision.
     * Only applicable for classification tasks.
     */
    double getPrecision();

    /**
     * Returns the macro-averaged recall.
     * Only applicable for classification tasks.
     */
    double getRecall();

    /**
     * Returns the macro-averaged F1 score.
     * Only applicable for classification tasks.
     */
    double getF1Score();

    /**
     * Returns the confusion matrix as a formatted string.
     * Only applicable for classification tasks.
     */
    String getConfusionMatrix();

    /**
     * Returns per-class metrics (precision, recall, F1).
     * Only applicable for classification tasks.
     */
    Map<String, ClassMetrics> getPerClassMetrics();

    // ==================== Regression Metrics ====================

    /**
     * Returns the R² score (coefficient of determination).
     * Only applicable for regression tasks.
     */
    double getR2Score();

    /**
     * Returns the Root Mean Squared Error.
     * Only applicable for regression tasks.
     */
    double getRMSE();

    /**
     * Returns the Mean Absolute Error.
     * Only applicable for regression tasks.
     */
    double getMAE();

    /**
     * Returns per-output dimension metrics for multi-output regression.
     * Only applicable for regression tasks.
     */
    Map<String, RegressionMetrics> getPerOutputMetrics();

    // ==================== Common Metrics ====================

    /**
     * Returns a human-readable summary of the evaluation results.
     */
    String getSummaryText();

    /**
     * Returns the number of samples in the test dataset.
     */
    int getTestDataSize();

    /**
     * Per-class metrics for classification.
     */
    interface ClassMetrics {
        double getPrecision();
        double getRecall();
        double getF1Score();
        int getSupport(); // number of samples of this class
    }

    /**
     * Per-output metrics for regression.
     */
    interface RegressionMetrics {
        double getR2();
        double getRMSE();
        double getMAE();
    }
}
