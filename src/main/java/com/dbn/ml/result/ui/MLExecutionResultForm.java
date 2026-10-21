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

package com.dbn.ml.result.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import com.dbn.ml.result.MLExecutionResult;
import com.dbn.ml.result.MLResult;
import com.dbn.ml.result.model.DBMSEvaluationResult;
import com.dbn.object.DBSchema;
import com.dbn.object.DBView;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.nls.NlsResources.txt;

/**
 * Form for displaying ML model evaluation results.
 *
 * @author ayoub allali
 */
@Slf4j
public class MLExecutionResultForm extends ExecutionResultFormBase<MLExecutionResult> {
    private static final int MAX_ANALYSIS_TABLE_ROWS = 12;

    // Form bindings
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel summaryPanel;
    private MLResultSummaryForm summaryForm;
    private DBNScrollPane contentScrollPane;
    private JPanel contentPanel;
    private JPanel metricsCardsPanel;
    private JPanel confusionMatrixPanel;
    private JPanel perClassPanel;
    private JPanel variableImportancePanel;
    private JPanel modelInsightsPanel;
    private JPanel modelViewsPanel;

    // Data
    private MLResult result;

    public MLExecutionResultForm(@NotNull MLExecutionResult executionResult) {
        super(executionResult);
        this.result = executionResult.getMlResult();
        initializeComponents();
    }

    private void initializeComponents() {
        initializeSummary();
        contentScrollPane.setVisible(!getExecutionResult().isLoading() && getExecutionResult().getLoadError() == null);
        if (contentScrollPane.isVisible()) initializeAnalysis();
        createActionsPanel();
    }

    private void initializeAnalysis() {
        for (JPanel panel : new JPanel[]{metricsCardsPanel, confusionMatrixPanel, perClassPanel,
                variableImportancePanel, modelInsightsPanel, modelViewsPanel}) {
            panel.removeAll();
            panel.setVisible(true);
        }
        initializeMetricsCards();
        initializeConfusionMatrix();
        initializePerClassMetrics();
        initializeFeatureImportance();
        initializeAttributeContribution();
        initializeModelViews();
    }

    private void createActionsPanel() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, false, "DBN.MachineLearning.Result");
        setAccessibleName(actionToolbar, txt("app.machineLearning.aria.MLExecutionResultActions"));
        actionsPanel.add(actionToolbar.getComponent());
    }

    public void refreshStatus() {
        summaryForm.refresh();
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public void refreshResult() {
        result = getExecutionResult().getMlResult();
        summaryForm.refresh();
        boolean available = !getExecutionResult().isLoading() && getExecutionResult().getLoadError() == null;
        contentScrollPane.setVisible(available);
        if (available) initializeAnalysis();
        mainPanel.revalidate();
        mainPanel.repaint();
    }

    public void refreshModelPresentation() {
        summaryForm.refresh();

        modelViewsPanel.removeAll();
        modelViewsPanel.setVisible(true);
        initializeModelViews();

        mainPanel.revalidate();
        mainPanel.repaint();
    }

    /**
     * Context for the header form - a reference to the trained model, which renders as
     * "connection - schema.model". Object references resolve their name lazily, so this does
     * not trigger a load of the AI model object list. Falls back to the plain connection when
     * the model is not identifiable (e.g. training failed before the model was created).
     */
    private Object getHeaderContext() {
        ConnectionHandler connection = result.getConnection();
        String modelName = result.getModelName();
        if (modelName == null || result.getModelHandle() == null) return connection;

        SchemaId schemaId = result.getModelHandle().getTargetSchemaId();
        DBSchema schema = schemaId == null ? connection.getUserSchema() : connection.getSchema(schemaId);
        if (schema == null) return connection;

        return new DBObjectRef<>(schema.ref(), DBObjectType.MINING_MODEL, modelName);
    }

    private void initializeMetricsCards() {
        metricsCardsPanel.setLayout(new GridLayout(1, 0, 0, 0));
        //metricsCardsPanel.setBorder(JBUI.Borders.empty(8));

        DBMSEvaluationResult evalResult = result.getEvaluationResult();
        if (evalResult == null) return;

        if (result.isClassification()) {
            addMetricCard(txt("app.machineLearning.label.Accuracy"), evalResult.getAccuracy(), true, "info/accuracy_info.html.ft");
            if (!evalResult.isConfusionMatrixDataOversized()) {
                addMetricCard(txt("app.machineLearning.label.Precision"), evalResult.getPrecision(), true, "info/precision_info.html.ft");
                addMetricCard(txt("app.machineLearning.label.Recall"), evalResult.getRecall(), true, "info/recall_info.html.ft");
                addMetricCard(txt("app.machineLearning.label.F1Score"), evalResult.getF1Score(), true, "info/f1_score_info.html.ft");
            }

            if (evalResult.getAucRoc() > 0) {
                addMetricCard(txt("app.machineLearning.label.AucRoc"), evalResult.getAucRoc(), true, "info/auc_roc_info.html.ft");
            }
        } else {
            addMetricCard(txt("app.machineLearning.label.R2Score"), evalResult.getR2Score(), true, "info/r2_score_info.html.ft");
            addMetricCard(txt("app.machineLearning.label.RMSE"), evalResult.getRMSE(), false, "info/rmse_info.html.ft");
            addMetricCard(txt("app.machineLearning.label.MAE"), evalResult.getMAE(), false, "info/mae_info.html.ft");
        }
    }

    private void addMetricCard(@Nls String name, double value, boolean isRatio, @NonNls String infoResourceName) {
        MLMetricCardPanel card = new MLMetricCardPanel(name, value, isRatio, infoResourceName);
        JPanel panel = new JPanel(new BorderLayout());
        if (metricsCardsPanel.getComponentCount() > 0) {
            panel.add(Box.createHorizontalStrut(JBUI.scale(12)), BorderLayout.WEST);
        }
        panel.add(card, BorderLayout.CENTER);
        metricsCardsPanel.add(panel);
    }

    private void initializeConfusionMatrix() {
        if (!result.isClassification()) {
            confusionMatrixPanel.setVisible(false);
            return;
        }

        MLResultPanelHelper.initSection(confusionMatrixPanel, txt("app.machineLearning.title.ConfusionMatrix"), "info/confusion_matrix_info.html.ft");

        // Try to get confusion matrix data
        Map<String, Integer> confusionData = null;
        DBMSEvaluationResult dbmsEval = result.getEvaluationResult();
        if (dbmsEval != null) {
            if (dbmsEval.isConfusionMatrixDataOversized()) {
                confusionMatrixPanel.add(new JLabel(txt("app.machineLearning.text.ConfusionMatrixNotAvailable")), BorderLayout.CENTER);
                return;
            }
            confusionData = dbmsEval.getConfusionMatrixData();
        }

        if (confusionData != null && !confusionData.isEmpty()) {
            confusionMatrixPanel.add(createHeatmapTable(confusionData), BorderLayout.CENTER);
        } else {
            // getConfusionMatrix() falls back to the localized "not applicable" placeholder
            String matrixText = result.getConfusionMatrix();
            if (matrixText != null && !matrixText.equals(txt("app.machineLearning.placeholder.NotApplicable"))) {
                JTextArea textArea = new JTextArea(matrixText);
                textArea.setEditable(false);
                textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
                confusionMatrixPanel.add(new JScrollPane(textArea), BorderLayout.CENTER);
            } else {
                confusionMatrixPanel.add(new JLabel(txt("app.machineLearning.text.ConfusionMatrixNotAvailable")), BorderLayout.CENTER);
            }
        }
    }

    private JComponent createHeatmapTable(Map<String, Integer> confusionData) {
        TreeSet<String> classLabels = new TreeSet<>();
        int maxCount = 0;
        int rowCount = 0;
        for (Map.Entry<String, Integer> entry : confusionData.entrySet()) {
            if (++rowCount > DBMSEvaluationResult.MAX_CONFUSION_MATRIX_ROWS) {
                return new JLabel(txt("app.machineLearning.text.ConfusionMatrixNotAvailable"));
            }
            String[] parts = entry.getKey().split("\0");
            if (parts.length >= 2) {
                if (parts[0].length() > DBMSEvaluationResult.MAX_CONFUSION_MATRIX_LABEL_LENGTH ||
                        parts[1].length() > DBMSEvaluationResult.MAX_CONFUSION_MATRIX_LABEL_LENGTH) {
                    return new JLabel(txt("app.machineLearning.text.ConfusionMatrixNotAvailable"));
                }
                classLabels.add(parts[0]);
                classLabels.add(parts[1]);
                if (classLabels.size() > DBMSEvaluationResult.MAX_CONFUSION_MATRIX_CLASSES) {
                    return new JLabel(txt("app.machineLearning.text.ConfusionMatrixNotAvailable"));
                }
                maxCount = Math.max(maxCount, entry.getValue());
            }
        }

        List<String> labels = new ArrayList<>(classLabels);
        int size = labels.size();
        if ((long) size * size > DBMSEvaluationResult.MAX_CONFUSION_MATRIX_CELLS) {
            return new JLabel(txt("app.machineLearning.text.ConfusionMatrixNotAvailable"));
        }

        String[] columns = new String[size + 1];
        columns[0] = txt("app.machineLearning.column.ActualPredicted");
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

        return createTablePanel(table);
    }

    private void initializePerClassMetrics() {
        if (!result.isClassification()) {
            perClassPanel.setVisible(false);
            return;
        }

        MLResultPanelHelper.initSection(perClassPanel, txt("app.machineLearning.title.PerClassPerformance"));

        JPanel chartPanel = new JPanel();
        chartPanel.setLayout(new BoxLayout(chartPanel, BoxLayout.Y_AXIS));

        DBMSEvaluationResult evalResult = result.getEvaluationResult();
        if (evalResult != null && !evalResult.isConfusionMatrixDataOversized()) {
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
            chartPanel.add(new JLabel(txt("app.machineLearning.text.PerClassMetricsNotAvailable")));
        }

        perClassPanel.add(chartPanel, BorderLayout.CENTER);
    }

    private void initializeSummary() {
        summaryForm = new MLResultSummaryForm(this, getExecutionResult());
        summaryPanel.add(summaryForm.getComponent(), BorderLayout.NORTH);
    }

    private void initializeFeatureImportance() {
        if (!result.hasFeatureImportance()) {
            variableImportancePanel.setVisible(false);
            return;
        }

        MLResultPanelHelper.initSection(variableImportancePanel,
                txt("app.machineLearning.title.FeatureImportance"),
                "info/feature_importance_info.html.ft");

        MLFeatureImportanceTableModel model = new MLFeatureImportanceTableModel(result.getFeatureImportance());
        variableImportancePanel.add(createAnalysisTablePanel(new MLAnalysisTable<>(this, model,
                txt("app.machineLearning.aria.FeatureImportanceTable"))), BorderLayout.CENTER);
    }

    private void initializeAttributeContribution() {
        if (!result.hasAttributeContribution()) {
            modelInsightsPanel.setVisible(false);
            return;
        }

        MLResultPanelHelper.initSection(modelInsightsPanel,
                txt("app.machineLearning.title.AttributeContribution"),
                "info/attribute_contribution_info.html.ft");

        MLAttributeContributionTableModel model = new MLAttributeContributionTableModel(result.getAttributeContributions());
        modelInsightsPanel.add(createAnalysisTablePanel(new MLAnalysisTable<>(this, model,
                txt("app.machineLearning.aria.AttributeContributionTable"))), BorderLayout.CENTER);
    }

    private static JPanel createTablePanel(JTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.add(table.getTableHeader(), BorderLayout.NORTH);
        panel.add(table, BorderLayout.CENTER);
        return panel;
    }

    /**
     * Caps analysis tables so a long result table cannot make the result dashboard excessively tall.
     * The inner scrollbar is intentional: it preserves the dashboard layout while retaining access
     * to every ranked attribute.
     */
    private static DBNScrollPane createAnalysisTablePanel(JTable table) {
        DBNScrollPane scrollPane = new DBNScrollPane(table);
        int visibleRows = Math.min(table.getRowCount(), MAX_ANALYSIS_TABLE_ROWS);
        int height = table.getTableHeader().getPreferredSize().height + visibleRows * table.getRowHeight();
        Dimension size = new Dimension(0, height);
        scrollPane.setPreferredSize(size);
        scrollPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
        return scrollPane;
    }

    private void initializeModelViews() {
        MLResultPanelHelper.initSection(modelViewsPanel, txt("app.machineLearning.title.ModelDetailViews"), "info/model_detail_views_info.html.ft");

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
        Map<String, String> modelViews = result.getModelDetailViews();
        if (modelViews == null) {
            addMutedLabel(linksPanel, txt("app.machineLearning.text.ModelDetailViewsLookupFailed"));
        } else if (modelViews.isEmpty()) {
            addMutedLabel(linksPanel, txt("app.machineLearning.text.NoModelDetailViews"));
        } else {
            for (Map.Entry<String, String> modelView : modelViews.entrySet()) {
                String viewName = modelView.getKey();
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
                row.setOpaque(false);

                DBNHyperlinkLabel link = new DBNHyperlinkLabel();
                link.setIcon(Icons.DBO_VIEW);
                link.setHyperlinkText(viewName);
                link.addHyperlinkListener(e -> openView(project, connection, viewName));

                row.add(link);
                String viewType = modelView.getValue();
                if (viewType != null && !viewType.isBlank()) {
                    JLabel typeLabel = new JLabel("— " + viewType);
                    typeLabel.setForeground(JBColor.gray);
                    row.add(typeLabel);
                }
                linksPanel.add(row);
            }
        }

        modelViewsPanel.add(linksPanel, BorderLayout.CENTER);
    }

    private static void addMutedLabel(JPanel panel, @Nls String text) {
        JLabel label = new JLabel(text);
        label.setForeground(JBColor.gray);
        panel.add(label);
    }

    private void openView(Project project, ConnectionHandler connection, String viewName) {
        SchemaId schemaId = result.getModelHandle().getTargetSchemaId();
        DBSchema schema = schemaId == null ? connection.getUserSchema() : connection.getSchema(schemaId);
        if (schema == null) return;

        DBObjectList<DBView> viewList = schema.getChildObjectList(DBObjectType.VIEW);
        if (viewList == null) return;

        ConnectionAction.invoke(txt("msg.machineLearning.title.OpeningView"), true, viewList,
                action -> Progress.prompt(project, viewList, true,
                        txt("msg.machineLearning.title.OpeningView"), txt("msg.machineLearning.text.LoadingView", viewName),
                        progress -> {
                            DBView view = schema.getView(viewName);
                            if (view == null) {
                                // The completion-time invalidation can be ignored when the view list is
                                // already loading. Reload once on a miss before reporting a stale cache.
                                viewList.reload();
                                view = schema.getView(viewName);
                            }

                            if (view != null) {
                                view.navigate(true);
                            } else {
                                showErrorDialog(project,
                                        txt("msg.machineLearning.title.RefreshRequired"),
                                        txt("msg.machineLearning.error.ViewNotVisible", viewName));
                            }
                        }));
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public MLResult getResult() {
        return result;
    }
}
