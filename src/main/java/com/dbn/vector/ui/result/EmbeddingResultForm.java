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

package com.dbn.vector.ui.result;

import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.vector.model.VectorEmbeddingExecutionResult;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.result.EmbeddingResult;
import com.intellij.openapi.actionSystem.ActionToolbar;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class EmbeddingResultForm extends ExecutionResultFormBase<VectorEmbeddingExecutionResult> {
  private JPanel mainPanel;
  private DBNScrollPane sourceDataScrollPane;
  private JPanel actionsPanel;
  private JPanel summaryPanel;
  private VectorEmbeddingResultsTable sourceDataTable;

  private final VectorEmbeddingResult result;

  public EmbeddingResultForm(@NotNull VectorEmbeddingExecutionResult executionResult) {
    super(executionResult);
    this.result = getExecutionResult().getVectorEmbeddingResult();
    initializeComponents();
  }

  private void initializeComponents() {
    initializeTable();
    initializeSummary();
    createActionsPanel();
  }

    private void initializeSummary() {
        EmbeddingResultSummaryForm summaryForm = new EmbeddingResultSummaryForm(this, result);
        summaryPanel.add(summaryForm.getComponent());
    }

  private void createActionsPanel() {
    ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, false, "DBNavigator.ActionGroup.VectorEmbeddingResult");
    setAccessibleName(actionToolbar, txt("app.execution.aria.VectorEmbeddingExecutionResultActions"));
    actionsPanel.add(actionToolbar.getComponent());
  }

/*
  private void initializeHeader() {
    metricsComponents.append("Duration: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    metricsComponents.append(TimeUtil.presentableDuration(result.getDuration(), true), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    metricsComponents.append(" • ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
    metricsComponents.append("Total Rows: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    metricsComponents.append(String.valueOf(result.getTotalInsertedRows()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    metricsComponents.append(" • ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
    metricsComponents.append("Sources: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    metricsComponents.append(String.valueOf(result.size()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    metricsComponents.append(" • ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
    metricsComponents.append("Success Rate: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    metricsComponents.append(result.getSuccessRate()+"%", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
  }
*/

  private void initializeTable() {

    VectorEmbeddingResultsTableModel sourceDataModel = new VectorEmbeddingResultsTableModel(result);
    sourceDataTable = new VectorEmbeddingResultsTable(this, sourceDataModel);
    sourceDataScrollPane.setViewportView(sourceDataTable);

    // Add selection listener to handle row selection and show pipeline/details
/*    sourceDataTable.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        pipelineHeaderPanel.setVisible(true);
        int viewRow = sourceDataTable.getSelectedRow();
        if (viewRow < 0) return;

        int modelRow = sourceDataTable.convertRowIndexToModel(viewRow);
        VectorEmbeddingResultsTableModel model = sourceDataTable.getModel();
        EmbeddingResult sr = model.getData(modelRow);
        String sourceN = Naming.shortenFileName(sr.getName(), 50);

        // Update source name and icon
        sourceName.setText(sourceN + "  ");
        sourceName.setIcon(sr.getIcon());


        // Only make it a hyperlink if it's a TableResult
        if (sr instanceof EmbeddingTableResult) {
          sourceName.setHyperlinkText(sourceN + "  ");
          sourceName.addHyperlinkListener((event) -> {
            openTableInEditor(sr);
          });
        } else {
          // For FileResult, just show as plain text (not clickable)
          sourceName.setText(sourceN + "  ");
        }

        updateStepStatus(sr);
        showPipelineDetails(sr);
      }
    });*/
  }

  private void openTableInEditor(EmbeddingResult embeddingResult) {
    ConnectionHandler connection = getResult().getConnection();
    String[] parts = embeddingResult.getIdentifier().split("\\.");

    if (parts.length != 2) {
      // Handle error - invalid identifier format
      return;
    }

    String schemaName = parts[0];
    String tableName = parts[1];

    DBSchema schema = connection.getSchema(connection.getSchemaId(schemaName));
    if (schema == null) {
      // Schema not found
      return;
    }

    DBTable table = schema.getTable(tableName);
    if (table == null) {
      // Table not found
      return;
    }

    DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(connection.getProject());
    editorManager.connectAndOpenEditor(table, null, true, true);
  }

/*  private void updateStepStatus(EmbeddingResult sr) {
    SourceStatus status = sr.getStatus();
    Icon icon = null;
    if (status == SourceStatus.FAILED) {
      icon = Icons.COMMON_STATUS_ERROR;
    }else if (status == SourceStatus.SUCCESS){
      icon = Icons.COMMON_STATUS_SUCCESS;
    }else if (status ==SourceStatus.RUNNING){
      icon = AllIcons.Process.Step_passive;
    }
    sourceStatusPanel.removeAll();
    sourceStatusPanel.add(new JLabel(icon));
  }

  private void showPipelineDetails(EmbeddingResult sr) {
    // Render pipeline steps for the selected SourceResult
    pipelinePanel.removeAll();

    List<StepResult> allSteps = new ArrayList<>();
    List<StepResult> steps = sr.getSteps();
    List<StepResult> sharedSteps = result.getSharedSteps();

    allSteps.addAll(sharedSteps);
    allSteps.addAll(steps);


    buildPipelineSteps(allSteps);


    pipelinePanel.revalidate();
    pipelinePanel.repaint();
  }

  private void buildPipelineSteps(List<StepResult> steps) {
    pipelinePanel.removeAll();
    int order = 0;
//    steps.forEach(step -> {
//      order++;
//      pipelinePanel.add(new PipelineStepForm(this,step).getComponent());
//     });
    for(StepResult step:steps){
      order++;
      pipelinePanel.add(new PipelineStepForm(this,step,order).getComponent());
    }

    pipelinePanel.revalidate();
    pipelinePanel.repaint();
  }
*/

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  public VectorEmbeddingResult getResult() {
    return result;
  }

  public EmbeddingResult getSelectedSource() {
    int viewRow = sourceDataTable.getSelectedRow();
    if (viewRow < 0) return null;

    int modelRow = sourceDataTable.convertRowIndexToModel(viewRow);
    VectorEmbeddingResultsTableModel model = sourceDataTable.getModel();
    return model.getData(modelRow);
  }
}
