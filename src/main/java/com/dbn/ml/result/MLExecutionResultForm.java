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

package com.dbn.ml.result;

import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.util.Actions;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.dbn.ml.backend.dbms.DBMSAlgorithmType;
import com.dbn.ml.backend.dbms.DBMSEvaluationResult;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import com.dbn.ml.model.MLModelDetails;
import com.dbn.ml.model.MLResult;
import com.dbn.ml.result.detail.AlgorithmDetailBuilder;
import com.dbn.ml.result.detail.DecisionTreeDetailBuilder;
import com.dbn.ml.result.detail.GLMDetailBuilder;
import com.dbn.ml.result.detail.NaiveBayesDetailBuilder;
import com.dbn.ml.result.detail.SVMDetailBuilder;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

/**
 * Form for displaying ML model evaluation results.
 *
 * @author ayoub allali
 */
public class MLExecutionResultForm extends ExecutionResultFormBase<MLExecutionResult> {

    private static final int BAR_HEIGHT = 4;

    private static final List<AlgorithmDetailBuilder> DETAIL_BUILDERS = List.of(
            new GLMDetailBuilder(),
            new SVMDetailBuilder(),
            new DecisionTreeDetailBuilder(),
            new NaiveBayesDetailBuilder());

    // Form bindings
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel headerPanel;
    private JPanel titleBar;
    private JLabel titleLabel;
    private JLabel taskTypeLabel;
    private JLabel scoreLabel;
    private com.intellij.ui.SimpleColoredComponent metricsSummary;
    private DBNScrollPane contentScrollPane;
    private JPanel contentPanel;
    private JPanel alertsPanel;
    private JPanel metricsCardsPanel;
    private JPanel confusionMatrixPanel;
    private JPanel perClassPanel;
    private JPanel modelDetailsPanel;
    private JPanel variableImportancePanel;
    private JPanel algorithmDetailsPanel;
    private JPanel modelInsightsPanel;

    // Data
    private final MLResult result;

    public MLExecutionResultForm(@NotNull MLExecutionResult executionResult) {
        super(executionResult);
        this.result = executionResult.getMlResult();
        initializeComponents();
    }

    private void initializeComponents() {
        initializeHeader();
        initializeBuildAlerts();
        initializeMetricsCards();
        initializeConfusionMatrix();
        initializePerClassMetrics();
        initializeModelDetails();
        initializeVariableImportance();
        initializeAlgorithmDetails();
        initializeModelInsights();
        createActionsPanel();
    }

    private void createActionsPanel() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, false, "DBNavigator.ActionGroup.MLExecutionResult");
        setAccessibleName(actionToolbar, "ML Execution Result Actions");
        actionsPanel.add(actionToolbar.getComponent());
    }

    private void initializeHeader() {
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 16f));

        // Set title with model name
        String modelName = result.getModelName();
        titleLabel.setText(modelName != null ? modelName : "ML Training Result");

        // Task type
        String taskType = result.isClassification() ? "Classification" : "Regression";
        taskTypeLabel.setText(taskType);
        taskTypeLabel.setForeground(JBColor.gray);

        // Overall score
        double score = calculateOverallScore();
        scoreLabel.setText(String.format("Score: %.0f", score));
        scoreLabel.setFont(scoreLabel.getFont().deriveFont(Font.BOLD));

        // Metrics summary line
        DBMSEvaluationResult evalResult = result.getEvaluationResult();
        if (evalResult != null) {
            if (result.isClassification()) {
                metricsSummary.append("Accuracy: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                metricsSummary.append(String.format("%.1f%%", evalResult.getAccuracy() * 100), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                metricsSummary.append(" | ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                metricsSummary.append("F1: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                metricsSummary.append(String.format("%.1f%%", evalResult.getF1Score() * 100), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            } else {
                metricsSummary.append("R\u00B2: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                metricsSummary.append(String.format("%.4f", evalResult.getR2Score()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
                metricsSummary.append(" | ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
                metricsSummary.append("RMSE: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
                metricsSummary.append(String.format("%.4f", evalResult.getRMSE()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            }
            metricsSummary.append(" | ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            metricsSummary.append("Algorithm: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            metricsSummary.append(result.getAlgorithmName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            metricsSummary.append(" | ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
            metricsSummary.append("Time: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            metricsSummary.append(result.getTrainingTimeMs() + "ms", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        }
    }

    private void initializeMetricsCards() {
        metricsCardsPanel.setLayout(new GridLayout(1, 0, 12, 0));
        metricsCardsPanel.setBorder(JBUI.Borders.empty(8));

        DBMSEvaluationResult evalResult = result.getEvaluationResult();
        if (evalResult == null) return;

        if (result.isClassification()) {
            metricsCardsPanel.add(new MLMetricCardPanel("Accuracy", evalResult.getAccuracy(), true));
            metricsCardsPanel.add(new MLMetricCardPanel("Precision", evalResult.getPrecision(), true));
            metricsCardsPanel.add(new MLMetricCardPanel("Recall", evalResult.getRecall(), true));
            metricsCardsPanel.add(new MLMetricCardPanel("F1 Score", evalResult.getF1Score(), true));

            if (evalResult.getAucRoc() > 0) {
                metricsCardsPanel.add(new MLMetricCardPanel("AUC-ROC", evalResult.getAucRoc(), true));
            }
        } else {
            metricsCardsPanel.add(new MLMetricCardPanel("R\u00B2 Score", evalResult.getR2Score(), true));
            metricsCardsPanel.add(new MLMetricCardPanel("RMSE", evalResult.getRMSE(), false));
            metricsCardsPanel.add(new MLMetricCardPanel("MAE", evalResult.getMAE(), false));
        }
    }

    private void initializeConfusionMatrix() {
        if (!result.isClassification()) {
            confusionMatrixPanel.setVisible(false);
            return;
        }

        MLResultPanelHelper.initSection(confusionMatrixPanel, "Confusion Matrix");

        // Try to get confusion matrix data
        Map<String, Integer> confusionData = null;
        DBMSEvaluationResult dbmsEval = result.getEvaluationResult();
        if (dbmsEval != null) {
            confusionData = dbmsEval.getConfusionMatrixData();
        }

        if (confusionData != null && !confusionData.isEmpty()) {
            confusionMatrixPanel.add(createHeatmapTable(confusionData), BorderLayout.CENTER);
        } else {
            String matrixText = result.getConfusionMatrix();
            if (matrixText != null && !matrixText.equals("N/A")) {
                JTextArea textArea = new JTextArea(matrixText);
                textArea.setEditable(false);
                textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                confusionMatrixPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
            } else {
                confusionMatrixPanel.add(new JLabel("Confusion matrix not available"), BorderLayout.CENTER);
            }
        }
    }

    private JComponent createHeatmapTable(Map<String, Integer> confusionData) {
        TreeSet<String> classLabels = new TreeSet<>();
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : confusionData.entrySet()) {
            String[] parts = entry.getKey().split("_");
            if (parts.length >= 2) {
                classLabels.add(parts[0]);
                classLabels.add(parts[1]);
                maxCount = Math.max(maxCount, entry.getValue());
            }
        }

        List<String> labels = new ArrayList<>(classLabels);
        int size = labels.size();

        String[] columns = new String[size + 1];
        columns[0] = "Actual / Predicted";
        for (int i = 0; i < size; i++) {
            columns[i + 1] = labels.get(i);
        }

        Object[][] data = new Object[size][size + 1];
        for (int i = 0; i < size; i++) {
            data[i][0] = labels.get(i);
            for (int j = 0; j < size; j++) {
                String key = labels.get(i) + "_" + labels.get(j);
                Integer count = confusionData.get(key);
                data[i][j + 1] = count != null ? count : 0;
            }
        }

        DefaultTableModel model = new DefaultTableModel(data, columns) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JBTable table = new JBTable(model);
        table.setRowHeight(36);
        table.getTableHeader().setReorderingAllowed(false);

        final int finalMaxCount = maxCount;
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);

                if (column > 0 && value instanceof Integer) {
                    int count = (Integer) value;
                    float intensity = finalMaxCount > 0 ? (float) count / finalMaxCount : 0;
                    if (count > 0) {
                        int alpha = (int)(20 + intensity * 80);
                        setBackground(new JBColor(
                            new Color(100, 100, 100, alpha),
                            new Color(180, 180, 180, alpha)));
                    } else {
                        setBackground(JBColor.background());
                    }
                } else {
                    setBackground(JBColor.background());
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                setForeground(JBColor.foreground());
                return c;
            }
        };

        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Don't wrap in JScrollPane - let parent scroll pane handle scrolling
        JPanel tablePanel = new JPanel(new BorderLayout());
        tablePanel.add(table.getTableHeader(), BorderLayout.NORTH);
        tablePanel.add(table, BorderLayout.CENTER);
        return tablePanel;
    }

    private void initializePerClassMetrics() {
        if (!result.isClassification()) {
            perClassPanel.setVisible(false);
            return;
        }

        MLResultPanelHelper.initSection(perClassPanel, "Per-Class Performance");

        JPanel chartPanel = new JPanel();
        chartPanel.setLayout(new BoxLayout(chartPanel, BoxLayout.Y_AXIS));

        DBMSEvaluationResult evalResult = result.getEvaluationResult();
        if (evalResult != null) {
            var perClassMetrics = evalResult.getPerClassMetrics();
            if (perClassMetrics != null && !perClassMetrics.isEmpty()) {
                for (var entry : perClassMetrics.entrySet()) {
                    var m = entry.getValue();
                    chartPanel.add(new MLClassRowPanel(entry.getKey(), m.getPrecision(), m.getRecall(), m.getF1Score(), m.getSupport()));
                    chartPanel.add(Box.createVerticalStrut(8));
                }
            }
        }

        if (chartPanel.getComponentCount() == 0) {
            chartPanel.add(new JLabel("Per-class metrics not available"));
        }

        perClassPanel.add(chartPanel, BorderLayout.CENTER);
    }

    private void initializeModelDetails() {
        MLResultPanelHelper.initSection(modelDetailsPanel, "Model Details");

        JPanel detailsGrid = new JPanel(new GridLayout(0, 4, 16, 6));

        addDetailRow(detailsGrid, "Algorithm", result.getAlgorithmName());
        addDetailRow(detailsGrid, "Features", String.valueOf(result.getFeatureCount()));
        addDetailRow(detailsGrid, "Training Samples", String.valueOf(result.getTrainingDataSize()));
        addDetailRow(detailsGrid, "Test Samples", String.valueOf(result.getTestingDataSize()));
        addDetailRow(detailsGrid, "Training Time", result.getTrainingTimeMs() + " ms");

        if (result.isClassification()) {
            addDetailRow(detailsGrid, "Classes", String.valueOf(result.getClassCount()));
        } else {
            addDetailRow(detailsGrid, "Output Dimensions", String.valueOf(result.getOutputDimensions()));
        }

        modelDetailsPanel.add(detailsGrid, BorderLayout.CENTER);
    }

    private void initializeVariableImportance() {
        MLModelDetails details = result.getModelDetails();
        if (details == null || !details.hasVariableImportance()) {
            variableImportancePanel.setVisible(false);
            return;
        }

        MLResultPanelHelper.initSection(variableImportancePanel, "Variable Importance");

        JPanel barsPanel = new JPanel();
        barsPanel.setLayout(new BoxLayout(barsPanel, BoxLayout.Y_AXIS));
        barsPanel.setBorder(JBUI.Borders.emptyTop(8));

        List<MLModelDetails.VariableImportance> items = details.getVariableImportance();
        double maxImportance = items.stream().mapToDouble(MLModelDetails.VariableImportance::getImportance).max().orElse(1.0);

        for (MLModelDetails.VariableImportance item : items) {
            barsPanel.add(createImportanceRow(item.getAttributeName(), item.getImportance(), maxImportance));
            barsPanel.add(Box.createVerticalStrut(6));
        }

        variableImportancePanel.add(barsPanel, BorderLayout.CENTER);
    }

    private JPanel createImportanceRow(String name, double importance, double maxImportance) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        row.setOpaque(false);

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(nameLabel.getFont().deriveFont(12f));
        nameLabel.setPreferredSize(new Dimension(180, 16));
        row.add(nameLabel, BorderLayout.WEST);

        int pct = maxImportance > 0 ? (int) (importance / maxImportance * 100) : 0;
        row.add(new MLProgressBarPanel(pct, BAR_HEIGHT), BorderLayout.CENTER);

        JLabel valLabel = new JLabel(String.format("%.3f", importance));
        valLabel.setFont(valLabel.getFont().deriveFont(Font.BOLD, 12f));
        valLabel.setPreferredSize(new Dimension(50, 16));
        valLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        row.add(valLabel, BorderLayout.EAST);

        return row;
    }

    private void initializeBuildAlerts() {
        MLModelDetails details = result.getModelDetails();
        if (details == null || !details.hasBuildAlerts()) {
            alertsPanel.setVisible(false);
            return;
        }

        alertsPanel.setLayout(new BorderLayout(8, 4));
        alertsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new JBColor(new Color(200, 140, 0), new Color(180, 130, 0)), 1),
                JBUI.Borders.empty(10)
        ));
        alertsPanel.setBackground(new JBColor(new Color(255, 248, 220), new Color(60, 50, 20)));
        alertsPanel.setOpaque(true);

        JLabel warningLabel = new JLabel("Build Warnings (" + details.getBuildAlerts().size() + ")");
        warningLabel.setFont(warningLabel.getFont().deriveFont(Font.BOLD, 13f));
        warningLabel.setForeground(new JBColor(new Color(160, 100, 0), new Color(220, 170, 60)));
        alertsPanel.add(warningLabel, BorderLayout.NORTH);

        JPanel alertsList = new JPanel();
        alertsList.setLayout(new BoxLayout(alertsList, BoxLayout.Y_AXIS));
        alertsList.setOpaque(false);
        alertsList.setBorder(JBUI.Borders.emptyTop(4));
        for (String alert : details.getBuildAlerts()) {
            JLabel alertLabel = new JLabel("\u26A0 " + alert);
            alertLabel.setFont(alertLabel.getFont().deriveFont(11f));
            alertLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
            alertsList.add(alertLabel);
            alertsList.add(Box.createVerticalStrut(2));
        }
        alertsPanel.add(alertsList, BorderLayout.CENTER);
    }

    private void initializeAlgorithmDetails() {
        MLModelDetails details = result.getModelDetails();
        if (details == null) {
            algorithmDetailsPanel.setVisible(false);
            return;
        }

        DBMSAlgorithmType algorithmType = null;
        try {
            algorithmType = DBMSAlgorithmType.fromDisplayName(result.getAlgorithmName());
        } catch (Exception ignored) {}

        DBMSAlgorithmType finalAlgorithmType = algorithmType;
        for (AlgorithmDetailBuilder builder : DETAIL_BUILDERS) {
            if (builder.canHandle(details, finalAlgorithmType)) {
                builder.build(algorithmDetailsPanel, details);
                return;
            }
        }
        algorithmDetailsPanel.setVisible(false);
    }

    private void initializeModelInsights() {
        MLModelDetails details = result.getModelDetails();
        if (details == null || (!details.hasGlobalStats() && !details.hasComputedSettings())) {
            modelInsightsPanel.setVisible(false);
            return;
        }

        modelInsightsPanel.setLayout(new GridLayout(1, details.hasGlobalStats() && details.hasComputedSettings() ? 2 : 1, 16, 0));
        modelInsightsPanel.setBorder(JBUI.Borders.empty(0));

        if (details.hasGlobalStats()) {
            modelInsightsPanel.add(buildInsightCard("Global Statistics", details.getGlobalStats()));
        }

        if (details.hasComputedSettings()) {
            // Filter to the most relevant settings (exclude verbose ones)
            Map<String, String> filteredSettings = new LinkedHashMap<>();
            details.getComputedSettings().forEach((k, v) -> {
                if (!k.startsWith("ODMS_DETAILS") && !k.equals("PREP_AUTO")) {
                    filteredSettings.put(k, v);
                }
            });
            if (!filteredSettings.isEmpty()) {
                modelInsightsPanel.add(buildInsightCard("Computed Settings", filteredSettings));
            }
        }
    }

    private JPanel buildInsightCard(String title, Map<String, String> data) {
        JPanel card = new JPanel(new BorderLayout(8, 6));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1),
                JBUI.Borders.empty(12)
        ));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        card.add(titleLabel, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 4));
        grid.setBorder(JBUI.Borders.emptyTop(6));
        data.forEach((k, v) -> {
            JLabel keyLabel = new JLabel(k);
            keyLabel.setForeground(JBColor.gray);
            keyLabel.setFont(keyLabel.getFont().deriveFont(11f));
            grid.add(keyLabel);

            JLabel valLabel = new JLabel(v);
            valLabel.setFont(valLabel.getFont().deriveFont(Font.BOLD, 11f));
            grid.add(valLabel);
        });
        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private void addDetailRow(JPanel panel, String label, String value) {
        JLabel labelComp = new JLabel(label + ":");
        labelComp.setForeground(JBColor.gray);
        panel.add(labelComp);

        JLabel valueComp = new JLabel(value);
        valueComp.setFont(valueComp.getFont().deriveFont(Font.BOLD));
        panel.add(valueComp);
    }

    private double calculateOverallScore() {
        DBMSEvaluationResult evalResult = result.getEvaluationResult();
        if (evalResult == null) return 0;

        if (result.isClassification()) {
            double accuracy = evalResult.getAccuracy();
            double f1 = evalResult.getF1Score();
            return (accuracy * 0.6 + f1 * 0.4) * 100;
        } else {
            return Math.max(0, evalResult.getR2Score()) * 100;
        }
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public MLResult getResult() {
        return result;
    }
}
