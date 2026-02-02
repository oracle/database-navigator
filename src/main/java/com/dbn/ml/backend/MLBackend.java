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

package com.dbn.ml.backend;

import com.dbn.ml.backend.model.MLEvaluationResult;
import com.dbn.ml.backend.model.MLModelHandle;
import com.dbn.ml.backend.model.MLPredictionResult;
import com.dbn.ml.backend.model.MLTrainingContext;
import com.dbn.ml.model.MLTaskType;

import java.util.List;
import java.util.Map;

/**
 * Main abstraction for ML training backends.
 * <p>
 * Implementations:
 * - TribuoBackend: Client-side training using Tribuo ML library
 * - DBMSBackend: In-database training using Oracle DBMS_DATA_MINING
 *
 * @author ayoub allali
 */
public interface MLBackend {

    /**
     * Returns the backend type.
     */
    MLBackendType getBackendType();

    /**
     * Trains a model using the provided context.
     *
     * @param context Contains source config, feature config, trainer config, connection
     * @return Handle to the trained model (in-memory or database reference)
     * @throws Exception if training fails
     */
    MLModelHandle train(MLTrainingContext context) throws Exception;

    /**
     * Evaluates a trained model.
     *
     * @param modelHandle The model to evaluate
     * @param context Training context (contains test data references)
     * @return Evaluation metrics
     * @throws Exception if evaluation fails
     */
    MLEvaluationResult evaluate(MLModelHandle modelHandle, MLTrainingContext context) throws Exception;

    /**
     * Makes a prediction using the model.
     *
     * @param modelHandle The trained model
     * @param featureValues Input feature values (feature name → value)
     * @return Prediction result
     * @throws Exception if prediction fails
     */
    MLPredictionResult predict(MLModelHandle modelHandle, Map<String, Double> featureValues) throws Exception;

    /**
     * Returns available algorithms for this backend and task type.
     *
     * @param taskType The ML task type (classification, regression, etc.)
     * @return List of algorithm names supported by this backend
     */
    List<String> getAvailableAlgorithms(MLTaskType taskType);

    /**
     * Cleanup resources (staging tables, temp data, etc.)
     *
     * @param context Training context containing resource references
     * @throws Exception if cleanup fails
     */
    void cleanup(MLTrainingContext context) throws Exception;
}
