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
import lombok.Getter;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import static com.dbn.nls.NlsResources.txt;

/**
 * Prediction result from an Oracle DBMS_DATA_MINING PREDICTION() query.
 *
 * @author Oracle
 */
@Getter
public class DBMSPredictionResult {

    private final MLTaskType taskType;

    // Classification
    private String predictedClass;
    private double confidence;

    // Regression
    private double predictedValue;

    private DBMSPredictionResult(MLTaskType taskType) {
        this.taskType = taskType;
    }

    /**
     * Creates a classification prediction result from SQL query.
     */
    public static DBMSPredictionResult fromClassification(ResultSet rs) throws SQLException {
        DBMSPredictionResult result = new DBMSPredictionResult(MLTaskType.CLASSIFICATION);

        if (rs.next()) {
            result.predictedClass = rs.getString("PREDICTION");
            result.confidence = rs.getDouble("PROBABILITY");
        }

        return result;
    }

    /**
     * Creates a regression prediction result from SQL query.
     */
    public static DBMSPredictionResult fromRegression(ResultSet rs) throws SQLException {
        DBMSPredictionResult result = new DBMSPredictionResult(MLTaskType.REGRESSION);

        if (rs.next()) {
            result.predictedValue = rs.getDouble("PREDICTION");
        }

        return result;
    }

    // ==================== Classification Prediction ====================


    public String getPredictedClass() {
        return predictedClass;
    }


    public double getConfidence() {
        return confidence;
    }


    public Map<String, Double> getClassProbabilities() {
        // DBMS_DATA_MINING PREDICTION_PROBABILITY returns probability for predicted class
        // Getting all class probabilities would require separate queries
        Map<String, Double> probabilities = new HashMap<>();
        if (predictedClass != null) {
            probabilities.put(predictedClass, confidence);
        }
        return probabilities;
    }

    // ==================== Regression Prediction ====================


    public double getPredictedValue() {
        return predictedValue;
    }


    public Map<String, Double> getPredictedValues() {
        // DBMS_DATA_MINING doesn't support multi-output regression in the same way
        Map<String, Double> values = new HashMap<>();
        values.put("prediction", predictedValue);
        return values;
    }

    // ==================== Common ====================


    public String getSummaryText() {
        if (taskType == MLTaskType.CLASSIFICATION) {
            return txt("app.machineLearning.text.PredictedClassSummary", predictedClass, String.format("%.2f", confidence * 100));
        } else {
            return txt("app.machineLearning.text.PredictedValueSummary", String.format("%.4f", predictedValue));
        }
    }
}
