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

import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.ml.backend.dbms.DBMSAlgorithmType;
import com.dbn.ml.backend.dbms.DBMSEvaluationResult;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import com.dbn.ml.model.MLResult;
import com.dbn.object.DBSchema;
import com.dbn.object.DBView;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class MLExecutionResultForm extends ExecutionResultFormBase<MLExecutionResult> {

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
    private JPanel modelViewsPanel;

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
        initializeModelViews();
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
            String[] parts = entry.getKey().split("\0");
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
                String key = labels.get(i) + "\0" + labels.get(j);
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

    private void initializeModelViews() {
        MLResultPanelHelper.initSection(modelViewsPanel, "Model Detail Views");

        JPanel linksPanel = new JPanel();
        linksPanel.setLayout(new BoxLayout(linksPanel, BoxLayout.Y_AXIS));
        linksPanel.setBorder(JBUI.Borders.emptyTop(8));
        linksPanel.setOpaque(false);

        String modelName = result.getModelName();
        DBMSModelHandle modelHandle = result.getModelHandle();
        if (modelName == null || modelHandle == null) {
            modelViewsPanel.setVisible(false);
            return;
        }

        ConnectionHandler connection = modelHandle.getConnection();
        Project project = connection.getProject();

        for (String[] entry : getModelViewEntries()) {
            String viewName = "DM$V" + entry[0] + modelName;
            String description = entry[1];

            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            row.setOpaque(false);

            DBNHyperlinkLabel link = new DBNHyperlinkLabel();
            link.setIcon(Icons.DBO_VIEW);
            link.setHyperlinkText(viewName);
            link.addHyperlinkListener(e -> openView(project, connection, viewName));

            JLabel desc = new JLabel("— " + description);
            desc.setForeground(JBColor.gray);
            desc.setFont(desc.getFont().deriveFont(11f));

            row.add(link);
            row.add(desc);
            linksPanel.add(row);
        }

        modelViewsPanel.add(linksPanel, BorderLayout.CENTER);
    }

    private List<String[]> getModelViewEntries() {
        // Universal views — created for every model, all algorithms
        List<String[]> views = new ArrayList<>();
        views.add(new String[]{"G", "Global Statistics"});
        views.add(new String[]{"S", "Computed Settings"});
        views.add(new String[]{"W", "Build Alerts"});

        // Classification adds target class distribution
        if (result.isClassification()) {
            views.add(new String[]{"T", "Target Map"});
        }

        // Algorithm-specific views
        DBMSAlgorithmType algorithmType = null;
        try { algorithmType = DBMSAlgorithmType.fromDisplayName(result.getAlgorithmName()); }
        catch (Exception e) { log.debug("Failed to resolve algorithm type for '{}'", result.getAlgorithmName(), e); }

        if (algorithmType != null) {
            switch (algorithmType) {
                case RANDOM_FOREST ->
                        // ADP has no effect on Random Forest — no DM$VN
                        views.add(new String[]{"A", "Variable Importance"});
                case DECISION_TREE -> {
                    // ADP has no effect on Decision Tree — no DM$VN
                    views.add(new String[]{"P", "Node Split Hierarchy"});
                    views.add(new String[]{"I", "Node Statistics"});
                }
                case LOGISTIC_REGRESSION, LINEAR_REGRESSION -> {
                    views.add(new String[]{"N", "Normalization & Missing Value Handling"});
                    views.add(new String[]{"D", "GLM Coefficients"});
                    views.add(new String[]{"A", "Row Diagnostics"});
                }
                case SVM_CLASSIFICATION, SVM_REGRESSION -> {
                    views.add(new String[]{"N", "Normalization & Missing Value Handling"});
                    views.add(new String[]{"L", "SVM Linear Coefficients"});
                }
                case NAIVE_BAYES -> {
                    // ADP bins Naive Bayes data — DM$VB is present
                    views.add(new String[]{"B", "Binning Boundaries"});
                    views.add(new String[]{"P", "Class Priors"});
                    views.add(new String[]{"V", "Conditional Probabilities"});
                }
                case NEURAL_NETWORK_CLASSIFICATION, NEURAL_NETWORK_REGRESSION -> {
                    views.add(new String[]{"N", "Normalization & Missing Value Handling"});
                    views.add(new String[]{"A", "Neuron Weights"});
                }
                case XGBOOST_CLASSIFICATION, XGBOOST_REGRESSION ->
                        // ADP has no effect on XGBoost — no DM$VN
                        views.add(new String[]{"I", "Attribute Importance"});
                default -> {}
            }
        }
        return views;
    }

    private void openView(Project project, ConnectionHandler connection, String viewName) {
        DBSchema schema = connection.getUserSchema();
        if (schema == null) return;

        DBObjectList<DBView> viewList = schema.getChildObjectList(DBObjectType.VIEW);
        if (viewList == null) return;

        ConnectionAction.invoke("Opening View", true, viewList,
                action -> Progress.prompt(project, viewList, true,
                        "Opening View", "Loading view " + viewName,
                        progress -> {
                            // schema.getView() auto-loads lazily on a progress thread (allowSyncLoad = true)
                            // No full reload needed - avoids the expensive refresh-all-elements cycle
                            DBView view = schema.getView(viewName);
                            if (view != null) {
                                view.navigate(true);
                            } else {
                                Dispatch.run(() -> Messages.showErrorDialog(project,
                                        "Refresh Required",
                                        "View '" + viewName + "' is not yet visible in the browser.\n" +
                                        "Please refresh the database connection and try again."));
                            }
                        }));
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
