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

package com.dbn.ml.backend.tribuo;

import com.dbn.ml.backend.model.MLPredictionResult;
import com.dbn.ml.model.MLTaskType;
import lombok.Getter;
import org.tribuo.Prediction;
import org.tribuo.classification.Label;
import org.tribuo.regression.Regressor;

import java.util.HashMap;
import java.util.Map;

/**
 * Tribuo implementation of MLPredictionResult.
 * Wraps Tribuo Prediction&lt;?&gt; objects.
 *
 * @author Oracle
 */
@Getter
public class TribuoPredictionResult implements MLPredictionResult {

    private final MLTaskType taskType;
    private final Object nativePrediction; // Prediction<Label> or Prediction<Regressor>

    private TribuoPredictionResult(MLTaskType taskType, Object nativePrediction) {
        this.taskType = taskType;
        this.nativePrediction = nativePrediction;
    }

    /**
     * Creates a classification prediction result.
     */
    public static TribuoPredictionResult fromClassification(Prediction<Label> prediction) {
        return new TribuoPredictionResult(MLTaskType.CLASSIFICATION, prediction);
    }

    /**
     * Creates a regression prediction result.
     */
    public static TribuoPredictionResult fromRegression(Prediction<Regressor> prediction) {
        return new TribuoPredictionResult(MLTaskType.REGRESSION, prediction);
    }

    // ==================== Classification Prediction ====================

    @Override
    public String getPredictedClass() {
        if (taskType == MLTaskType.CLASSIFICATION) {
            Prediction<Label> prediction = (Prediction<Label>) nativePrediction;
            return prediction.getOutput().getLabel();
        }
        return null;
    }

    @Override
    public double getConfidence() {
        if (taskType == MLTaskType.CLASSIFICATION) {
            Prediction<Label> prediction = (Prediction<Label>) nativePrediction;
            Label predictedLabel = prediction.getOutput();

            // Get the probability of the predicted class
            var scores = prediction.getOutputScores();
            for (var entry : scores.entrySet()) {
                // Key is the class name (String), value is a Label with the score
                if (entry.getKey().equals(predictedLabel.getLabel())) {
                    return entry.getValue().getScore();
                }
            }
        }
        return 0.0;
    }

    @Override
    public Map<String, Double> getClassProbabilities() {
        Map<String, Double> probabilities = new HashMap<>();

        if (taskType == MLTaskType.CLASSIFICATION) {
            Prediction<Label> prediction = (Prediction<Label>) nativePrediction;
            var scores = prediction.getOutputScores();

            for (var entry : scores.entrySet()) {
                // Key is the class name (String), value is a Label with the score
                String className = entry.getKey();
                double probability = entry.getValue().getScore();
                probabilities.put(className, probability);
            }
        }

        return probabilities;
    }

    // ==================== Regression Prediction ====================

    @Override
    public double getPredictedValue() {
        if (taskType == MLTaskType.REGRESSION) {
            Prediction<Regressor> prediction = (Prediction<Regressor>) nativePrediction;
            Regressor regressor = prediction.getOutput();

            // For single-output regression, return the first dimension's value
            if (regressor.size() == 1) {
                return regressor.getValues()[0];
            } else {
                // For multi-output, return average (or could throw exception)
                double sum = 0.0;
                for (double value : regressor.getValues()) {
                    sum += value;
                }
                return sum / regressor.size();
            }
        }
        return 0.0;
    }

    @Override
    public Map<String, Double> getPredictedValues() {
        Map<String, Double> values = new HashMap<>();

        if (taskType == MLTaskType.REGRESSION) {
            Prediction<Regressor> prediction = (Prediction<Regressor>) nativePrediction;
            Regressor regressor = prediction.getOutput();

            String[] names = regressor.getNames();
            double[] outputValues = regressor.getValues();

            for (int i = 0; i < names.length; i++) {
                values.put(names[i], outputValues[i]);
            }
        }

        return values;
    }

    // ==================== Common ====================

    @Override
    public String getSummaryText() {
        if (taskType == MLTaskType.CLASSIFICATION) {
            return String.format("Predicted: %s (Confidence: %.2f%%)",
                    getPredictedClass(), getConfidence() * 100);
        } else {
            Map<String, Double> values = getPredictedValues();
            if (values.size() == 1) {
                return String.format("Predicted: %.4f", getPredictedValue());
            } else {
                StringBuilder sb = new StringBuilder("Predicted: ");
                int i = 0;
                for (Map.Entry<String, Double> entry : values.entrySet()) {
                    if (i > 0) sb.append(", ");
                    sb.append(String.format("%s=%.4f", entry.getKey(), entry.getValue()));
                    i++;
                }
                return sb.toString();
            }
        }
    }
}
