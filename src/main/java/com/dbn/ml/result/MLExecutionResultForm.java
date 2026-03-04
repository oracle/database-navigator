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
import com.dbn.ml.backend.dbms.DBMSEvaluationResult;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import com.dbn.ml.model.MLResult;
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
    private JPanel metricsCardsPanel;
    private JPanel confusionMatrixPanel;
    private JPanel perClassPanel;
    private JPanel modelDetailsPanel;

    // Data
    private final MLResult result;

    public MLExecutionResultForm(@NotNull MLExecutionResult executionResult) {
        super(executionResult);
        this.result = executionResult.getMlResult();
        initializeComponents();
    }

    private void initializeComponents() {
        initializeHeader();
        initializeMetricsCards();
        initializeConfusionMatrix();
        initializePerClassMetrics();
        initializeModelDetails();
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
            metricsCardsPanel.add(createMetricCard("Accuracy", evalResult.getAccuracy(), true));
            metricsCardsPanel.add(createMetricCard("Precision", evalResult.getPrecision(), true));
            metricsCardsPanel.add(createMetricCard("Recall", evalResult.getRecall(), true));
            metricsCardsPanel.add(createMetricCard("F1 Score", evalResult.getF1Score(), true));

            if (evalResult.getAucRoc() > 0) {
                metricsCardsPanel.add(createMetricCard("AUC-ROC", evalResult.getAucRoc(), true));
            }
        } else {
            metricsCardsPanel.add(createMetricCard("R\u00B2 Score", evalResult.getR2Score(), true));
            metricsCardsPanel.add(createMetricCard("RMSE", evalResult.getRMSE(), false));
            metricsCardsPanel.add(createMetricCard("MAE", evalResult.getMAE(), false));
        }
    }

    private JPanel createMetricCard(String name, double value, boolean isRatio) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        //
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1),
                JBUI.Borders.empty(12)
        ));

        JLabel nameLabel = new JLabel(name);
        nameLabel.setFont(nameLabel.getFont().deriveFont(11f));
        nameLabel.setForeground(JBColor.gray);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(nameLabel);
        card.add(Box.createVerticalStrut(4));

        String valueStr = isRatio ? String.format("%.1f%%", value * 100) : String.format("%.4f", value);
        JLabel valueLabel = new JLabel(valueStr);
        valueLabel.setFont(valueLabel.getFont().deriveFont(Font.BOLD, 18f));
        valueLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(valueLabel);

        if (isRatio) {
            card.add(Box.createVerticalStrut(6));
            JPanel bar = createProgressBar((int) (value * 100), BAR_HEIGHT);
            bar.setAlignmentX(Component.LEFT_ALIGNMENT);
            card.add(bar);
        }

        return card;
    }

    private void initializeConfusionMatrix() {
        if (!result.isClassification()) {
            confusionMatrixPanel.setVisible(false);
            return;
        }

        confusionMatrixPanel.setLayout(new BorderLayout(8, 8));
        confusionMatrixPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1),
                JBUI.Borders.empty(12)
        ));

        JLabel titleLabel = new JLabel("Confusion Matrix");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        confusionMatrixPanel.add(titleLabel, BorderLayout.NORTH);

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

        perClassPanel.setLayout(new BorderLayout(8, 8));
        perClassPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1),
                JBUI.Borders.empty(12)
        ));

        JLabel titleLabel = new JLabel("Per-Class Performance");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        perClassPanel.add(titleLabel, BorderLayout.NORTH);

        JPanel chartPanel = new JPanel();
        chartPanel.setLayout(new BoxLayout(chartPanel, BoxLayout.Y_AXIS));

        DBMSEvaluationResult evalResult = result.getEvaluationResult();
        if (evalResult != null) {
            var perClassMetrics = evalResult.getPerClassMetrics();
            if (perClassMetrics != null && !perClassMetrics.isEmpty()) {
                for (var entry : perClassMetrics.entrySet()) {
                    var m = entry.getValue();
                    chartPanel.add(createClassRow(entry.getKey(), m.getPrecision(), m.getRecall(), m.getF1Score(), m.getSupport()));
                    chartPanel.add(Box.createVerticalStrut(8));
                }
            }
        }

        if (chartPanel.getComponentCount() == 0) {
            chartPanel.add(new JLabel("Per-class metrics not available"));
        }

        perClassPanel.add(chartPanel, BorderLayout.CENTER);
    }

    private JPanel createClassRow(String className, double precision, double recall, double f1, int support) {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));

        // Left: class name and support
        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JLabel nameLabel = new JLabel(className);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 12f));
        leftPanel.add(nameLabel);
        JLabel supportLabel = new JLabel("(n=" + support + ")");
        supportLabel.setForeground(JBColor.gray);
        supportLabel.setFont(supportLabel.getFont().deriveFont(10f));
        leftPanel.add(supportLabel);
        leftPanel.setPreferredSize(new Dimension(180, 24));
        row.add(leftPanel, BorderLayout.WEST);

        // Center: metrics spread across available width
        JPanel metricsPanel = new JPanel(new GridLayout(1, 3, 20, 0));
        metricsPanel.add(createMetricBar("P", precision));
        metricsPanel.add(createMetricBar("R", recall));
        metricsPanel.add(createMetricBar("F1", f1));
        row.add(metricsPanel, BorderLayout.CENTER);

        return row;
    }

    private JPanel createMetricBar(String label, double value) {
        JPanel panel = new JPanel(new BorderLayout(6, 0));

        // Label on left
        JLabel labelComp = new JLabel(label);
        labelComp.setFont(labelComp.getFont().deriveFont(11f));
        labelComp.setForeground(JBColor.gray);
        labelComp.setPreferredSize(new Dimension(20, 16));
        panel.add(labelComp, BorderLayout.WEST);

        // Bar in center - wrap to keep fixed height
        JPanel barWrapper = new JPanel(new GridBagLayout());
        barWrapper.setOpaque(false);
        JPanel bar = createProgressBar((int) (value * 100), BAR_HEIGHT);
        barWrapper.add(bar, new GridBagConstraints() {{
            fill = GridBagConstraints.HORIZONTAL;
            weightx = 1.0;
        }});
        panel.add(barWrapper, BorderLayout.CENTER);

        // Value on right
        JLabel valueLabel = new JLabel(String.format("%.0f%%", value * 100));
        valueLabel.setFont(valueLabel.getFont().deriveFont(11f));
        valueLabel.setPreferredSize(new Dimension(40, 16));
        valueLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        panel.add(valueLabel, BorderLayout.EAST);

        return panel;
    }

    private JPanel createProgressBar(int percentage, int height) {
        return new JPanel() {
            {
                setPreferredSize(new Dimension(60, height));
                setMinimumSize(new Dimension(30, height));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth();
                int h = getHeight();
                int fillWidth = (int) (width * percentage / 100.0);

                // Background track
                g2.setColor(JBColor.border());
                g2.fillRoundRect(0, 0, width, h, h, h);

                // Filled portion
                if (fillWidth > 0) {
                    g2.setColor(new JBColor(new Color(130, 130, 130), new Color(160, 160, 160)));
                    g2.fillRoundRect(0, 0, fillWidth, h, h, h);
                }

                g2.dispose();
            }
        };
    }

    private void initializeModelDetails() {
        modelDetailsPanel.setLayout(new BorderLayout(8, 8));
        modelDetailsPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(JBColor.border(), 1),
                JBUI.Borders.empty(12)
        ));

        JLabel titleLabel = new JLabel("Model Details");
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 14f));
        modelDetailsPanel.add(titleLabel, BorderLayout.NORTH);

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
