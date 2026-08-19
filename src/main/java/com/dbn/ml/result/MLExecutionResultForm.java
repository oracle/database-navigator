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
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.ml.backend.dbms.DBMSEvaluationResult;
import com.dbn.ml.backend.dbms.DBMSModelHandle;
import com.dbn.ml.model.MLResult;
import com.dbn.object.DBSchema;
import com.dbn.object.DBView;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import lombok.extern.slf4j.Slf4j;
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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.nls.NlsResources.txt;

/**
 * Form for displaying ML model evaluation results.
 *
 * @author ayoub allali
 */
@Slf4j
public class MLExecutionResultForm extends ExecutionResultFormBase<MLExecutionResult> {
    private static final @NonNls String MODEL_VIEW_PREFIX = "DM$V";

    // Form bindings
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel headerPanel;
    private JPanel objectHeaderPanel;
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
        hideDetailedModelPanels();
        initializeModelViews();
        createActionsPanel();
    }

    private void hideDetailedModelPanels() {
        alertsPanel.setVisible(false);
        variableImportancePanel.setVisible(false);
        algorithmDetailsPanel.setVisible(false);
        modelInsightsPanel.setVisible(false);
    }

    private void createActionsPanel() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, false, "DBN.MachineLearning.Result");
        setAccessibleName(actionToolbar, txt("app.machineLearning.aria.MLExecutionResultActions"));
        actionsPanel.add(actionToolbar.getComponent());
    }

    private void initializeHeader() {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, getHeaderContext());
        objectHeaderPanel.add(headerForm.getComponent(), BorderLayout.CENTER);

        // Summary line - training context only, the metrics are shown as cards below
        metricsSummary.append(result.isClassification() ?
                        txt("app.machineLearning.const.MLTaskType_CLASSIFICATION") :
                        txt("app.machineLearning.const.MLTaskType_REGRESSION"),
                SimpleTextAttributes.REGULAR_ATTRIBUTES);
        metricsSummary.append(" | ", SimpleTextAttributes.GRAYED_ATTRIBUTES);

        String algorithmName = result.getAlgorithmName();
        if (algorithmName != null) {
            metricsSummary.append(txt("app.machineLearning.label.Algorithm") + ": ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            metricsSummary.append(algorithmName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
            metricsSummary.append(" | ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
        metricsSummary.append(txt("app.machineLearning.label.Time") + ": ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
        metricsSummary.append(presentableDuration(result.getTrainingTimeMs(), true), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
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
        if (modelName == null) return connection;

        DBSchema schema = connection.getUserSchema();
        if (schema == null) return connection;

        return new DBObjectRef<>(schema.ref(), DBObjectType.AI_MODEL, modelName);
    }

    private void initializeMetricsCards() {
        metricsCardsPanel.setLayout(new GridLayout(1, 0, 12, 0));
        metricsCardsPanel.setBorder(JBUI.Borders.empty(8));

        DBMSEvaluationResult evalResult = result.getEvaluationResult();
        if (evalResult == null) return;

        if (result.isClassification()) {
            metricsCardsPanel.add(new MLMetricCardPanel(txt("app.machineLearning.label.Accuracy"), evalResult.getAccuracy(), true, "info/accuracy_info.html.ft"));
            metricsCardsPanel.add(new MLMetricCardPanel(txt("app.machineLearning.label.Precision"), evalResult.getPrecision(), true, "info/precision_info.html.ft"));
            metricsCardsPanel.add(new MLMetricCardPanel(txt("app.machineLearning.label.Recall"), evalResult.getRecall(), true, "info/recall_info.html.ft"));
            metricsCardsPanel.add(new MLMetricCardPanel(txt("app.machineLearning.label.F1Score"), evalResult.getF1Score(), true, "info/f1_score_info.html.ft"));

            if (evalResult.getAucRoc() > 0) {
                metricsCardsPanel.add(new MLMetricCardPanel(txt("app.machineLearning.label.AucRoc"), evalResult.getAucRoc(), true, "info/auc_roc_info.html.ft"));
            }
        } else {
            metricsCardsPanel.add(new MLMetricCardPanel(txt("app.machineLearning.label.R2Score"), evalResult.getR2Score(), true, "info/r2_score_info.html.ft"));
            metricsCardsPanel.add(new MLMetricCardPanel(txt("app.machineLearning.label.RMSE"), evalResult.getRMSE(), false, "info/rmse_info.html.ft"));
            metricsCardsPanel.add(new MLMetricCardPanel(txt("app.machineLearning.label.MAE"), evalResult.getMAE(), false, "info/mae_info.html.ft"));
        }
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

        MLResultPanelHelper.initSection(perClassPanel, txt("app.machineLearning.title.PerClassPerformance"));

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
            chartPanel.add(new JLabel(txt("app.machineLearning.text.PerClassMetricsNotAvailable")));
        }

        perClassPanel.add(chartPanel, BorderLayout.CENTER);
    }

    private void initializeModelDetails() {
        MLResultPanelHelper.initSection(modelDetailsPanel, txt("app.machineLearning.title.ModelDetails"));

        JPanel detailsGrid = new JPanel(new GridLayout(0, 4, 16, 6));

        addDetailRow(detailsGrid, txt("app.machineLearning.label.Algorithm"), result.getAlgorithmName());
        addDetailRow(detailsGrid, txt("app.machineLearning.label.Features"), String.valueOf(result.getFeatureCount()));
        addDetailRow(detailsGrid, txt("app.machineLearning.label.TrainingSamples"), String.valueOf(result.getTrainingDataSize()));
        addDetailRow(detailsGrid, txt("app.machineLearning.label.TestSamples"), String.valueOf(result.getTestingDataSize()));
        addDetailRow(detailsGrid, txt("app.machineLearning.label.TrainingTime"), presentableDuration(result.getTrainingTimeMs(), false));

        if (result.isClassification()) {
            addDetailRow(detailsGrid, txt("app.machineLearning.label.Classes"), String.valueOf(result.getClassCount()));
        } else {
            addDetailRow(detailsGrid, txt("app.machineLearning.label.OutputDimensions"), String.valueOf(result.getOutputDimensions()));
        }

        modelDetailsPanel.add(detailsGrid, BorderLayout.CENTER);
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
        List<String> modelViews = loadModelViewNames(connection, modelName);
        if (modelViews.isEmpty()) {
            JLabel emptyLabel = new JLabel(txt("app.machineLearning.text.NoModelDetailViews"));
            emptyLabel.setForeground(JBColor.gray);
            linksPanel.add(emptyLabel);
        } else {
            for (String viewName : modelViews) {
                JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
                row.setOpaque(false);

                DBNHyperlinkLabel link = new DBNHyperlinkLabel();
                link.setIcon(Icons.DBO_VIEW);
                link.setHyperlinkText(viewName);
                link.addHyperlinkListener(e -> openView(project, connection, viewName));

                row.add(link);
                linksPanel.add(row);
            }
        }

        modelViewsPanel.add(linksPanel, BorderLayout.CENTER);
    }

    private List<String> loadModelViewNames(ConnectionHandler connection, String modelName) {
        DBSchema schema = connection.getUserSchema();
        if (schema == null || modelName == null || modelName.isBlank()) return List.of();

        String normalizedModelName = modelName.toUpperCase();
        List<String> names = new ArrayList<>();

        try {
            for (DBView view : schema.getViews()) {
                String viewName = view.getName();
                if (viewName == null) continue;

                if (isModelDetailView(viewName.toUpperCase(), normalizedModelName)) {
                    names.add(viewName);
                }
            }
            names.sort(String.CASE_INSENSITIVE_ORDER);
        } catch (Exception e) {
            log.debug("Failed to load model detail views for model '{}'", modelName, e);
        }

        return names;
    }

    /**
     * Model detail views are named DM$V + one algorithm specific letter + the model name.
     * The name after that letter must match exactly, otherwise the views of a model like
     * CUSTOMER_MODEL would also show up for a model named MODEL.
     */
    private static boolean isModelDetailView(String viewName, String modelName) {
        if (!viewName.startsWith(MODEL_VIEW_PREFIX)) return false;

        int modelNameStart = MODEL_VIEW_PREFIX.length() + 1;
        if (viewName.length() <= modelNameStart) return false;

        return viewName.substring(modelNameStart).equals(modelName);
    }

    private void openView(Project project, ConnectionHandler connection, String viewName) {
        DBSchema schema = connection.getUserSchema();
        if (schema == null) return;

        DBObjectList<DBView> viewList = schema.getChildObjectList(DBObjectType.VIEW);
        if (viewList == null) return;

        ConnectionAction.invoke(txt("msg.machineLearning.title.OpeningView"), true, viewList,
                action -> Progress.prompt(project, viewList, true,
                        txt("msg.machineLearning.title.OpeningView"), txt("msg.machineLearning.text.LoadingView", viewName),
                        progress -> {
                            // schema.getView() auto-loads lazily on a progress thread (allowSyncLoad = true)
                            // No full reload needed - avoids the expensive refresh-all-elements cycle
                            DBView view = schema.getView(viewName);
                            if (view != null) {
                                view.navigate(true);
                            } else {
                                Dispatch.run(() -> Messages.showErrorDialog(project,
                                        txt("msg.machineLearning.title.RefreshRequired"),
                                        txt("msg.machineLearning.error.ViewNotVisible", viewName)));
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

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public MLResult getResult() {
        return result;
    }
}
