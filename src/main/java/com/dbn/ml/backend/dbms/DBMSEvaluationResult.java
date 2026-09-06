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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.dbn.nls.NlsResources.txt;
import static java.lang.String.format;

/**
 * Evaluation metrics for an Oracle DBMS_DATA_MINING model.
 * Parses results from Oracle DM$ views and calculates
 * per-class precision/recall/F1 from confusion matrix data.
 *
 * @author Oracle
 */
@Getter
public class DBMSEvaluationResult {

    public static final String ILBR = "\n  "; // indented line break

    /**
     * Per-class metrics for classification.
     */
    public interface ClassMetrics {
        double getPrecision();
        double getRecall();
        double getF1Score();
        int getSupport();
    }

    /**
     * Per-output metrics for regression.
     */
    public interface RegressionMetrics {
        double getR2();
        double getRMSE();
        double getMAE();
    }

    private final MLTaskType taskType;
    private final int testDataSize;

    // Classification metrics
    private double accuracy;
    private double aucRoc;
    private final Map<String, Integer> confusionMatrixData;

    // Calculated per-class metrics (lazy initialized)
    private Map<String, ClassMetrics> perClassMetricsCache;
    private double macroPrecision;
    private double macroRecall;
    private double macroF1;

    // Regression metrics
    private double r2Score;
    private double rmse;
    private double mae;

    // Lift analysis data (for binary classification)
    private final List<LiftData> liftAnalysis;

    private DBMSEvaluationResult(MLTaskType taskType, int testDataSize) {
        this.taskType = taskType;
        this.testDataSize = testDataSize;
        this.confusionMatrixData = new HashMap<>();
        this.liftAnalysis = new ArrayList<>();
    }

    /**
     * Lift analysis data point for a quantile.
     */
    @Getter
    public static class LiftData {
        private final int quantile;
        private final double probabilityThreshold;
        private final double cumulativeGain;
        private final double cumulativeLift;
        private final int cumulativeTargets;
        private final int cumulativeNonTargets;

        public LiftData(int quantile, double probabilityThreshold, double cumulativeGain,
                       double cumulativeLift, int cumulativeTargets, int cumulativeNonTargets) {
            this.quantile = quantile;
            this.probabilityThreshold = probabilityThreshold;
            this.cumulativeGain = cumulativeGain;
            this.cumulativeLift = cumulativeLift;
            this.cumulativeTargets = cumulativeTargets;
            this.cumulativeNonTargets = cumulativeNonTargets;
        }
    }

    /**
     * Creates a classification evaluation result from Oracle's COMPUTE_CONFUSION_MATRIX output.
     * This is the proper Oracle-recommended approach using DBMS_DATA_MINING evaluation procedures.
     *
     * @param accuracy Accuracy returned by COMPUTE_CONFUSION_MATRIX (as decimal, e.g., 0.85)
     * @param confusionRs ResultSet from confusion matrix table (columns: ACTUAL_TARGET_VALUE, PREDICTED_TARGET_VALUE, VALUE)
     * @param auc AUC from COMPUTE_ROC (null if not binary classification)
     * @param testDataSize Number of test samples
     */
    public static DBMSEvaluationResult fromOracleEvaluation(
            double accuracy,
            ResultSet confusionRs,
            Double auc,
            int testDataSize) throws SQLException {

        DBMSEvaluationResult result = new DBMSEvaluationResult(MLTaskType.CLASSIFICATION, testDataSize);

        // Accuracy from COMPUTE_CONFUSION_MATRIX (already as decimal)
        result.accuracy = accuracy;
        result.aucRoc = auc != null ? auc : 0.0;

        // Parse confusion matrix from Oracle's output (columns: ACTUAL_TARGET_VALUE, PREDICTED_TARGET_VALUE, VALUE)
        while (confusionRs.next()) {
            String actual = confusionRs.getString("ACTUAL_TARGET_VALUE");
            String predicted = confusionRs.getString("PREDICTED_TARGET_VALUE");
            int count = confusionRs.getInt("VALUE");
            result.confusionMatrixData.put(actual + "\0" + predicted, count);
        }

        return result;
    }

    /**
     * Populates lift analysis data from Oracle's COMPUTE_LIFT output.
     * Should be called after creating the result from fromOracleEvaluation.
     *
     * @param liftRs ResultSet from lift table (columns: QUANTILE_NUMBER, PROBABILITY_THRESHOLD,
     *               GAIN_CUMULATIVE, LIFT_CUMULATIVE, TARGETS_CUMULATIVE, NON_TARGETS_CUMULATIVE)
     */
    public void populateLiftData(ResultSet liftRs) throws SQLException {
        while (liftRs.next()) {
            liftAnalysis.add(new LiftData(
                    liftRs.getInt("QUANTILE_NUMBER"),
                    liftRs.getDouble("PROBABILITY_THRESHOLD"),
                    liftRs.getDouble("GAIN_CUMULATIVE"),
                    liftRs.getDouble("LIFT_CUMULATIVE"),
                    liftRs.getInt("TARGETS_CUMULATIVE"),
                    liftRs.getInt("NON_TARGETS_CUMULATIVE")
            ));
        }
    }

    /**
     * Returns true if lift analysis data is available.
     */
    public boolean hasLiftData() {
        return !liftAnalysis.isEmpty();
    }

    /**
     * Creates a classification evaluation result by applying model to data (legacy method).
     * Uses direct PREDICTION() SQL function instead of COMPUTE_CONFUSION_MATRIX.
     */
    public static DBMSEvaluationResult fromClassification(
            ResultSet accuracyRs,
            ResultSet confusionRs,
            int testDataSize) throws SQLException {

        DBMSEvaluationResult result = new DBMSEvaluationResult(MLTaskType.CLASSIFICATION, testDataSize);

        // Parse accuracy metrics (columns: total_count, correct_count, accuracy_pct)
        if (accuracyRs.next()) {
            result.accuracy = accuracyRs.getDouble("accuracy_pct") / 100.0; // Convert percentage to decimal
            result.aucRoc = 0.0; // Not available with direct prediction approach
        }

        // Parse confusion matrix (columns: actual_target_value, predicted_target_value, value)
        while (confusionRs.next()) {
            String actual = confusionRs.getString("actual_target_value");
            String predicted = confusionRs.getString("predicted_target_value");
            int count = confusionRs.getInt("value");
            result.confusionMatrixData.put(actual + "\0" + predicted, count);
        }

        return result;
    }

    /**
     * Creates a regression evaluation result by applying model to data.
     */
    public static DBMSEvaluationResult fromRegression(
            ResultSet metricsRs,
            int testDataSize) throws SQLException {

        DBMSEvaluationResult result = new DBMSEvaluationResult(MLTaskType.REGRESSION, testDataSize);

        // Parse regression metrics (columns: sample_count, r_squared, rmse, mae)
        if (metricsRs.next()) {
            result.r2Score = metricsRs.getDouble("r_squared");
            result.rmse = metricsRs.getDouble("rmse");
            result.mae = metricsRs.getDouble("mae");
        }

        return result;
    }

    // ==================== Classification Metrics ====================

    public double getPrecision() {
        ensurePerClassMetricsCalculated();
        return macroPrecision;
    }


    public double getRecall() {
        ensurePerClassMetricsCalculated();
        return macroRecall;
    }


    public double getF1Score() {
        ensurePerClassMetricsCalculated();
        return macroF1;
    }


    public String getConfusionMatrix() {
        if (confusionMatrixData.isEmpty()) {
            return txt("app.machineLearning.placeholder.NotApplicable");
        }

        StringBuilder sb = new StringBuilder();
        sb.append(txt("app.machineLearning.text.ConfusionMatrixHeader"));

        // Format confusion matrix data
        for (Map.Entry<String, Integer> entry : confusionMatrixData.entrySet()) {
            String[] parts = entry.getKey().split("\0");
            sb.append(ILBR);
            sb.append(txt("app.machineLearning.text.ConfusionMatrixEntry", parts[0], parts[1], entry.getValue()));
        }

        sb.append('\n');
        return sb.toString();
    }


    public Map<String, ClassMetrics> getPerClassMetrics() {
        ensurePerClassMetricsCalculated();
        return perClassMetricsCache;
    }

    /**
     * Lazily calculates per-class metrics from the confusion matrix data.
     * This is called automatically when getPrecision(), getRecall(), getF1Score(),
     * or getPerClassMetrics() is invoked.
     */
    private void ensurePerClassMetricsCalculated() {
        if (perClassMetricsCache != null) {
            return; // Already calculated
        }

        perClassMetricsCache = new HashMap<>();

        if (confusionMatrixData.isEmpty()) {
            macroPrecision = 0.0;
            macroRecall = 0.0;
            macroF1 = 0.0;
            return;
        }

        // Extract all unique class labels
        Set<String> classLabels = new HashSet<>();
        for (String key : confusionMatrixData.keySet()) {
            String[] parts = key.split("\0");
            if (parts.length >= 2) {
                classLabels.add(parts[0]); // actual
                classLabels.add(parts[1]); // predicted
            }
        }

        // Calculate TP, FP, FN for each class
        double totalPrecision = 0.0;
        double totalRecall = 0.0;
        double totalF1 = 0.0;
        int validClasses = 0;

        for (String classLabel : classLabels) {
            int tp = 0; // True Positives: predicted as this class AND actually this class
            int fp = 0; // False Positives: predicted as this class BUT actually different
            int fn = 0; // False Negatives: actually this class BUT predicted different
            int support = 0; // Total actual samples of this class

            for (Map.Entry<String, Integer> entry : confusionMatrixData.entrySet()) {
                String[] parts = entry.getKey().split("\0");
                if (parts.length < 2) continue;

                String actual = parts[0];
                String predicted = parts[1];
                int count = entry.getValue();

                if (actual.equals(classLabel)) {
                    support += count;
                    if (predicted.equals(classLabel)) {
                        tp += count; // Correctly predicted this class
                    } else {
                        fn += count; // Should have predicted this class but didn't
                    }
                } else if (predicted.equals(classLabel)) {
                    fp += count; // Incorrectly predicted this class
                }
            }

            // Calculate metrics for this class
            double precision = (tp + fp) > 0 ? (double) tp / (tp + fp) : 0.0;
            double recall = (tp + fn) > 0 ? (double) tp / (tp + fn) : 0.0;
            double f1 = (precision + recall) > 0 ? 2 * precision * recall / (precision + recall) : 0.0;

            perClassMetricsCache.put(classLabel, new DBMSClassMetrics(precision, recall, f1, support));

            // Accumulate for macro average
            if (support > 0) {
                totalPrecision += precision;
                totalRecall += recall;
                totalF1 += f1;
                validClasses++;
            }
        }

        // Calculate macro averages
        if (validClasses > 0) {
            macroPrecision = totalPrecision / validClasses;
            macroRecall = totalRecall / validClasses;
            macroF1 = totalF1 / validClasses;
        } else {
            macroPrecision = 0.0;
            macroRecall = 0.0;
            macroF1 = 0.0;
        }
    }

    /**
     * Implementation of ClassMetrics for DBMS evaluation results.
     */
    @Getter
    public static class DBMSClassMetrics implements ClassMetrics {
        private final double precision;
        private final double recall;
        private final double f1Score;
        private final int support;

        public DBMSClassMetrics(double precision, double recall, double f1Score, int support) {
            this.precision = precision;
            this.recall = recall;
            this.f1Score = f1Score;
            this.support = support;
        }
    }

    // ==================== Regression Metrics ====================


    public double getRMSE() {
        return rmse;
    }


    public double getMAE() {
        return mae;
    }


    public Map<String, RegressionMetrics> getPerOutputMetrics() {
        // DBMS_DATA_MINING doesn't support multi-output regression in the same way
        return new HashMap<>();
    }

    // ==================== Common Metrics ====================


    public String getSummaryText() {
        StringBuilder sb = new StringBuilder();

        if (taskType == MLTaskType.CLASSIFICATION) {
            sb.append(txt("app.machineLearning.text.ClassificationEvaluationHeader"));
            sb.append(ILBR);
            sb.append(txt("app.machineLearning.text.AccuracySummary",
                    format("%.4f", accuracy),
                    format("%.2f", accuracy * 100)));
            if (aucRoc > 0) {
                sb.append(ILBR);
                sb.append(txt("app.machineLearning.text.AucRocSummary", format("%.4f", aucRoc)));
            }

            // Include calculated macro-averaged metrics
            ensurePerClassMetricsCalculated();
            sb.append(ILBR);
            sb.append(txt("app.machineLearning.text.MacroPrecisionSummary", format("%.4f", macroPrecision))).append(ILBR);
            sb.append(txt("app.machineLearning.text.MacroRecallSummary", format("%.4f", macroRecall))).append(ILBR);
            sb.append(txt("app.machineLearning.text.MacroF1Summary", format("%.4f", macroF1))).append(ILBR);
            sb.append(txt("app.machineLearning.text.TestDataSizeSummary", testDataSize));
            sb.append('\n');

            // Per-class breakdown
            if (!perClassMetricsCache.isEmpty()) {
                sb.append('\n').append("  ")
                        .append(txt("app.machineLearning.text.PerClassMetricsHeader"))
                        .append('\n');
                for (Map.Entry<String, ClassMetrics> entry : perClassMetricsCache.entrySet()) {
                    ClassMetrics m = entry.getValue();
                    sb.append("    ");
                    sb.append(txt("app.machineLearning.text.PerClassMetricsSummary",
                            entry.getKey(),
                            format("%.3f", m.getPrecision()),
                            format("%.3f", m.getRecall()),
                            format("%.3f", m.getF1Score()),
                            m.getSupport()));
                    sb.append('\n');
                }
            }
        } else {
            sb.append(txt("app.machineLearning.text.RegressionEvaluationHeader")).append(ILBR);
            sb.append(txt("app.machineLearning.text.R2ScoreSummary", format("%.4f", r2Score))).append(ILBR);
            sb.append(txt("app.machineLearning.text.RmseSummary", format("%.4f", rmse))).append(ILBR);
            sb.append(txt("app.machineLearning.text.MaeSummary", format("%.4f", mae))).append(ILBR);
            sb.append(txt("app.machineLearning.text.TestDataSizeSummary", testDataSize));
            sb.append('\n');
        }

        return sb.toString();
    }
}
