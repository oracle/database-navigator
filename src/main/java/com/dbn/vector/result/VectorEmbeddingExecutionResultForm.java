package com.dbn.vector.result;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.util.Naming;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.vector.model.SourceResult;
import com.dbn.vector.model.SourceStatus;
import com.dbn.vector.model.StepResult;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.intellij.icons.AllIcons;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.List;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;
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
  private com.intellij.ui.SimpleColoredComponent metricsLabel;
  private JPanel pipelinePanel;
  private JPanel pipelineHeaderPanel;
  private JSplitPane contentSplitPane;
  private JLabel sourceName;
  private JPanel sourceStatusPanel;
  private VectorEmbeddingSourcesTable sourceDataTable;

  public VectorEmbeddingExecutionResultForm(@NotNull VectorEmbeddingExecutionResult executionResult) {
    super(executionResult);
    System.out.println("ff");
    this.result = getExecutionResult().getVectorEmbeddingResult();
    verticalBoxLayout(pipelinePanel);
    initializeComponents();
    setSplitPaneProportion(contentSplitPane, 0.4);
  }

  private void initializeComponents() {
    pipelineHeaderPanel.setVisible(false);

    initializeTable();
    initializeHeader();
  }

  private void initializeHeader() {
    switch (result.getStatus()){
      case SUCCESS:
        statusBadge.setIcon(Icons.COMMON_STATUS_SUCCESS);
//        this.statusBadge.setText(result.getStatus());
        break;
        case FAILED:
          statusBadge.setIcon(Icons.COMMON_STATUS_ERROR);
          break;
      case PARTIAL:
        statusBadge.setIcon(Icons.COMMON_WARNING);
    }
    statusBadge.setText("");

    metricsLabel.append("Duration: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    metricsLabel.append((double)result.getDuration() / 1000 +"s", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    metricsLabel.append(" • ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
    metricsLabel.append("Total Rows: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    metricsLabel.append(String.valueOf(result.getTotalInsertedRows()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    metricsLabel.append(" • ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
    metricsLabel.append("Sources: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    metricsLabel.append(String.valueOf(result.getSourceResults().size()), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    metricsLabel.append(" • ", SimpleTextAttributes.GRAYED_ATTRIBUTES);
    metricsLabel.append("Success Rate: ", SimpleTextAttributes.REGULAR_ATTRIBUTES);
    metricsLabel.append(result.getSuccessRate()+"%", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
//    this.durationValue.setText((double)result.getDuration() / 1000 +"s");
//    this.totalRowsValue.setText(String.valueOf(result.getTotalInsertedRows()));
//    this.filesValue.setText(String.valueOf(result.getSourceResults().size()));
//    this.successRateValue.setText(result.getSuccessRate()+"%");
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

    List<StepResult> steps = sr.getSteps();
//    if (steps != null) {
      buildPipelineSteps(steps);
//    } else {
//      // Fallback: show textual summary if steps are missing
//      JPanel content = new JPanel(new BorderLayout());
//      JTextArea ta = new JTextArea();
//      ta.setEditable(false);
//      ta.setLineWrap(true);
//      ta.setWrapStyleWord(true);
//
//      StringBuilder sb = new StringBuilder();
//      sb.append("Source: ").append(sr.getDisplayName()).append("\n");
//      sb.append("Status: ").append(sr.getStatus()).append("\n");
//      sb.append("Rows: ").append(sr.getRowsInserted()).append("\n\n");
//
//      sb.append("Details:\n");
//      sb.append(sr.toString());
//
//      ta.setText(sb.toString());
//      content.add(new JScrollPane(ta), BorderLayout.CENTER);
//      pipelinePanel.add(content);
//    }

    pipelinePanel.revalidate();
    pipelinePanel.repaint();
  }

  private void buildPipelineSteps(List<StepResult> steps) {
    pipelinePanel.removeAll();
    steps.forEach(step -> {
//      addPipelineStep(step.getStep().getDisplayName(), null, step.getStatus());

      pipelinePanel.add(new PipelineStepForm(this,step).getComponent());
     });

    pipelinePanel.revalidate();
    pipelinePanel.repaint();
  }

  private void addPipelineStep(String name, Icon icon, StepResult.STEP_STATUS status) {
    JPanel step = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    step.setBorder(JBUI.Borders.empty(4, 10));

    JBColor bg, fg;
    switch (status) {
      case SUCCEEDED:
        bg = new JBColor(new Color(0x2F9E44), new Color(0x2B8A3E));
        fg = new JBColor(Color.WHITE, Color.WHITE);
        break;
      case RUNNING:
        bg = new JBColor(new Color(0x4C6EF5), new Color(0x4263EB));
        fg = new JBColor(Color.WHITE, Color.WHITE);
        break;
      case FAILED:
        bg = new JBColor(new Color(0xC92A2A), new Color(0xA61E1E));
        fg = new JBColor(Color.WHITE, Color.WHITE);
        break;
      default: // PENDING
        bg = new JBColor(new Color(0x495057), new Color(0x343A40));
        fg = new JBColor(new Color(0xADB5BD), new Color(0x868E96));
    }

    step.setBackground(bg);
    step.setOpaque(true);

    JBLabel iconLabel = new JBLabel(icon);
    JBLabel textLabel = new JBLabel(name);
    textLabel.setForeground(fg);
    textLabel.setFont(textLabel.getFont().deriveFont(Font.BOLD, 11f));

    JBLabel statusIcon = new JBLabel(getStatusIcon(status));

    step.add(iconLabel);
    step.add(textLabel);
    step.add(statusIcon);

    pipelinePanel.add(step);
  }

  private void addArrow() {
    JBLabel arrow = new JBLabel(AllIcons.General.ArrowRight);
    arrow.setBorder(JBUI.Borders.empty(0, 2));
    pipelinePanel.add(arrow);
  }

  private Icon getStatusIcon(StepResult.STEP_STATUS status) {
    switch (status) {
      case SUCCEEDED: return AllIcons.Actions.Checked;
      case RUNNING: return AllIcons.Process.Step_1;
      case FAILED: return AllIcons.Ide.FatalError;
      default: return AllIcons.General.Gear;
    }
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  public VectorEmbeddingResult getResult() {
    return result;
  }
}
