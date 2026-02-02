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
 * Backend-agnostic prediction result.
 *
 * @author Oracle
 */
public interface MLPredictionResult {

    /**
     * Returns the task type of this prediction.
     */
    MLTaskType getTaskType();

    // ==================== Classification Prediction ====================

    /**
     * Returns the predicted class label.
     * Only applicable for classification tasks.
     */
    String getPredictedClass();

    /**
     * Returns the confidence/probability of the predicted class (0.0 to 1.0).
     * Only applicable for classification tasks.
     */
    double getConfidence();

    /**
     * Returns probabilities for all classes.
     * Only applicable for classification tasks.
     */
    Map<String, Double> getClassProbabilities();

    // ==================== Regression Prediction ====================

    /**
     * Returns the predicted value (for single-output regression).
     * Only applicable for regression tasks.
     */
    double getPredictedValue();

    /**
     * Returns predicted values for multi-output regression.
     * Key: output dimension name, Value: predicted value.
     * Only applicable for regression tasks with multiple outputs.
     */
    Map<String, Double> getPredictedValues();

    /**
     * Returns a human-readable summary of the prediction.
     */
    String getSummaryText();
}
