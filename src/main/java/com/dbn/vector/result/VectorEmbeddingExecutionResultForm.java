package com.dbn.vector.result;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Naming;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.vector.model.SourceResult;
import com.dbn.vector.model.SourceStatus;
import com.dbn.vector.model.StepResult;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Splitters.setSplitPaneProportion;

public class VectorEmbeddingExecutionResultForm extends ExecutionResultFormBase<VectorEmbeddingExecutionResult> {
  private final VectorEmbeddingResult result;
  private JPanel mainPanel;
  private JPanel headerPanel;
  private JPanel sourceDataPanel;
  private JPanel embeddingDetailsPanel;
  private DBNScrollPane sourceDataScrollPane;
  private JPanel titleBar;
  private JLabel titleLabel;
  private JLabel statusBadge;
  private JPanel metricsPanel;
  private com.intellij.ui.SimpleColoredComponent metricsComponents;
  private JPanel pipelinePanel;
  private JPanel pipelineHeaderPanel;
  private JSplitPane contentSplitPane;
  private JLabel sourceName;
  private JPanel sourceStatusPanel;
  private JPanel actionsPanel;
  private VectorEmbeddingSourcesTable sourceDataTable;

  public VectorEmbeddingExecutionResultForm(@NotNull VectorEmbeddingExecutionResult executionResult) {
    super(executionResult);
    this.result = getExecutionResult().getVectorEmbeddingResult();
    verticalBoxLayout(pipelinePanel);
    initializeComponents();
    setSplitPaneProportion(contentSplitPane, 0.4);
  }

  private void initializeComponents() {
    pipelineHeaderPanel.setVisible(false);

    initializeTable();
    initializeHeader();
    createActionsPanel();
  }

  private void createActionsPanel() {
    ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, false, "DBNavigator.ActionGroup.VectorEmbeddingExecutionResult");
    setAccessibleName(actionToolbar, txt("app.execution.aria.VectorEmbeddingExecutionResultActions"));
    actionsPanel.add(actionToolbar.getComponent());
  }

  private void initializeHeader() {
    switch (result.getStatus()){
      case SUCCESS:
        statusBadge.setIcon(Icons.COMMON_STATUS_SUCCESS);
        statusBadge.setToolTipText(String.format("All %d sources embedded successfully",
                result.getSourceResults().size()));
        break;
      case FAILED:
        statusBadge.setIcon(Icons.COMMON_STATUS_ERROR);
        statusBadge.setToolTipText(String.format("Embedding failed - 0 of %d sources processed",
                result.getSourceResults().size()));
        break;
      case PARTIAL:
        statusBadge.setIcon(Icons.COMMON_WARNING);
        statusBadge.setToolTipText(String.format("Partial success - %d of %d sources embedded",
                result.getSourceSucceedCount(),  result.getSourceResults().size()));
        break;
    }
    statusBadge.setText("");

    metricsComponents.append("Duration: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    metricsComponents.append(String.format("%.1f",(double)result.getDuration()/1000)+"s", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    metricsComponents.append(" • ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
    metricsComponents.append("Total Rows: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    metricsComponents.append(String.valueOf(result.getTotalInsertedRows()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    metricsComponents.append(" • ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
    metricsComponents.append("Sources: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    metricsComponents.append(String.valueOf(result.getSourceResults().size()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    metricsComponents.append(" • ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
    metricsComponents.append("Success Rate: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    metricsComponents.append(result.getSuccessRate()+"%", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);

  }

  private void initializeTable() {

    VectorEmbeddingSourcesTableModel sourceDataModel = new VectorEmbeddingSourcesTableModel(result.getSourceResults());
    sourceDataTable = new VectorEmbeddingSourcesTable(this, sourceDataModel);
    sourceDataScrollPane.setViewportView(sourceDataTable);

    // Add selection listener to handle row selection and show pipeline/details
    sourceDataTable.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        pipelineHeaderPanel.setVisible(true);
        int viewRow = sourceDataTable.getSelectedRow();
        if (viewRow < 0) return;

        int modelRow = sourceDataTable.convertRowIndexToModel(viewRow);
        VectorEmbeddingSourcesTableModel model = sourceDataTable.getModel();
        SourceResult sr = model.getSourceResults().get(modelRow);
        String sourceN  = Naming.shortenFileName(sr.getName(),50);
        sourceName.setText(sourceN+"  ");
        updateStepStatus(sr);
        showPipelineDetails(sr);
      }
    });
  }

  private void updateStepStatus(SourceResult sr) {
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

  private void showPipelineDetails(SourceResult sr) {
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


  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  public VectorEmbeddingResult getResult() {
    return result;
  }
}
