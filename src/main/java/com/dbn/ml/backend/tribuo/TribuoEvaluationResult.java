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

import com.dbn.ml.backend.model.MLEvaluationResult;
import com.dbn.ml.model.MLTaskType;
import lombok.Getter;
import org.tribuo.classification.Label;
import org.tribuo.classification.evaluation.LabelEvaluation;
import org.tribuo.regression.Regressor;
import org.tribuo.regression.evaluation.RegressionEvaluation;

import java.util.HashMap;
import java.util.Map;

/**
 * Tribuo implementation of MLEvaluationResult.
 * Wraps Tribuo evaluation objects (LabelEvaluation or RegressionEvaluation).
 *
 * @author Oracle
 */
@Getter
public class TribuoEvaluationResult implements MLEvaluationResult {

    private final MLTaskType taskType;
    private final Object nativeEvaluation; // LabelEvaluation or RegressionEvaluation
    private final int testDataSize;

    public TribuoEvaluationResult(LabelEvaluation evaluation, int testDataSize) {
        this.taskType = MLTaskType.CLASSIFICATION;
        this.nativeEvaluation = evaluation;
        this.testDataSize = testDataSize;
    }

    public TribuoEvaluationResult(RegressionEvaluation evaluation, int testDataSize) {
        this.taskType = MLTaskType.REGRESSION;
        this.nativeEvaluation = evaluation;
        this.testDataSize = testDataSize;
    }

    // ==================== Classification Metrics ====================

    @Override
    public double getAccuracy() {
        if (taskType == MLTaskType.CLASSIFICATION) {
            return ((LabelEvaluation) nativeEvaluation).accuracy();
        }
        return 0.0;
    }

    @Override
    public double getPrecision() {
        if (taskType == MLTaskType.CLASSIFICATION) {
            return ((LabelEvaluation) nativeEvaluation).macroAveragedPrecision();
        }
        return 0.0;
    }

    @Override
    public double getRecall() {
        if (taskType == MLTaskType.CLASSIFICATION) {
            return ((LabelEvaluation) nativeEvaluation).macroAveragedRecall();
        }
        return 0.0;
    }

    @Override
    public double getF1Score() {
        if (taskType == MLTaskType.CLASSIFICATION) {
            return ((LabelEvaluation) nativeEvaluation).macroAveragedF1();
        }
        return 0.0;
    }

    @Override
    public String getConfusionMatrix() {
        if (taskType == MLTaskType.CLASSIFICATION) {
            return ((LabelEvaluation) nativeEvaluation).getConfusionMatrix().toString();
        }
        return "N/A";
    }

    @Override
    public Map<String, ClassMetrics> getPerClassMetrics() {
        Map<String, ClassMetrics> metricsMap = new HashMap<>();

        if (taskType == MLTaskType.CLASSIFICATION) {
            LabelEvaluation evaluation = (LabelEvaluation) nativeEvaluation;

            // Get domain from confusion matrix
            var confusionMatrix = evaluation.getConfusionMatrix();
            for (Label label : confusionMatrix.getDomain().getDomain()) {
                String className = label.getLabel();

                // Get metrics for this label using evaluation methods
                final double precision = evaluation.precision(label);
                final double recall = evaluation.recall(label);
                final double f1 = evaluation.f1(label);
                final double support = confusionMatrix.support(label);

                metricsMap.put(className, new ClassMetrics() {
                    @Override
                    public double getPrecision() {
                        return precision;
                    }

                    @Override
                    public double getRecall() {
                        return recall;
                    }

                    @Override
                    public double getF1Score() {
                        return f1;
                    }

                    @Override
                    public int getSupport() {
                        return (int) support;
                    }
                });
            }
        }

        return metricsMap;
    }

    // ==================== Regression Metrics ====================

    @Override
    public double getR2Score() {
        if (taskType == MLTaskType.REGRESSION) {
            var r2Map = ((RegressionEvaluation) nativeEvaluation).r2();
            return r2Map.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
        return 0.0;
    }

    @Override
    public double getRMSE() {
        if (taskType == MLTaskType.REGRESSION) {
            var rmseMap = ((RegressionEvaluation) nativeEvaluation).rmse();
            return rmseMap.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
        return 0.0;
    }

    @Override
    public double getMAE() {
        if (taskType == MLTaskType.REGRESSION) {
            var maeMap = ((RegressionEvaluation) nativeEvaluation).mae();
            return maeMap.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        }
        return 0.0;
    }

    @Override
    public Map<String, RegressionMetrics> getPerOutputMetrics() {
        Map<String, RegressionMetrics> metricsMap = new HashMap<>();

        if (taskType == MLTaskType.REGRESSION) {
            RegressionEvaluation evaluation = (RegressionEvaluation) nativeEvaluation;

            var r2Map = evaluation.r2();
            var rmseMap = evaluation.rmse();
            var maeMap = evaluation.mae();

            // Keys are Regressor objects, not Strings
            for (Regressor regressor : r2Map.keySet()) {
                String dimensionName = regressor.getNames()[0]; // Get first dimension name
                final double r2 = r2Map.getOrDefault(regressor, 0.0);
                final double rmse = rmseMap.getOrDefault(regressor, 0.0);
                final double mae = maeMap.getOrDefault(regressor, 0.0);

                metricsMap.put(dimensionName, new RegressionMetrics() {
                    @Override
                    public double getR2() {
                        return r2;
                    }

                    @Override
                    public double getRMSE() {
                        return rmse;
                    }

                    @Override
                    public double getMAE() {
                        return mae;
                    }
                });
            }
        }

        return metricsMap;
    }

    // ==================== Common Metrics ====================

    @Override
    public String getSummaryText() {
        if (taskType == MLTaskType.CLASSIFICATION) {
            return ((LabelEvaluation) nativeEvaluation).toString();
        } else {
            return ((RegressionEvaluation) nativeEvaluation).toString();
        }
    }

    @Override
    public int getTestDataSize() {
        return testDataSize;
    }

    /**
     * Returns the native Tribuo evaluation object.
     */
    public Object getNativeEvaluation() {
        return nativeEvaluation;
    }
}
