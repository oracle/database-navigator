package com.dbn.vector.result;

import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.execution.common.result.ui.ExecutionResultFormBase;
import com.dbn.vector.model.SourceResult;
import com.dbn.vector.model.SourceStatus;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.intellij.icons.AllIcons;

import com.dbn.vector.model.StepResult;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;

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
  private JLabel durationLabel;
  private JLabel durationValue;
  private JLabel totalRowsLabel;
  private JLabel totalRowsValue;
  private JLabel filesLabel;
  private JLabel filesValue;
  private JLabel successRateLabel;
  private JLabel successRateValue;
  private JPanel pipelinePanel;
  private JTable sourceDataTable;
  private DBNScrollPane DBNScrollPane1;

  public VectorEmbeddingExecutionResultForm(@NotNull VectorEmbeddingExecutionResult executionResult) {
    super(executionResult);
    this.result = getExecutionResult().getVectorEmbeddingResult();
    verticalBoxLayout(pipelinePanel);
    initializeComponents();

  }

  private void initializeComponents() {
    initializeTable();
  }

  private void initializeTable() {
    sourceDataTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    sourceDataTable.setShowGrid(true);

    sourceDataTable.setModel(new SourceDataModel (result.getSourceResults()));

    // Custom renderer for status column
      sourceDataTable.getColumnModel().getColumn(0).setCellRenderer(new StatusCellRenderer());


    // Add selection listener to handle row selection and show pipeline/details
    sourceDataTable.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        int viewRow = sourceDataTable.getSelectedRow();
        if (viewRow < 0) return;

        int modelRow = sourceDataTable.convertRowIndexToModel(viewRow);
        SourceDataModel model = (SourceDataModel) sourceDataTable.getModel();
        SourceResult sr = model.sourceResults.get(modelRow);

        showPipelineDetails(sr);
      }
    });
  }

  private static class SourceDataModel extends AbstractTableModel {
    List<SourceResult> sourceResults;
    private static final String[] COLS = {
            "Source Name", "Rows Embedded"
    };

    public SourceDataModel(List<SourceResult> sourceResults) {
      this.sourceResults = sourceResults;
    }

    @Override public int getRowCount() { return sourceResults.size(); }
    @Override public int getColumnCount() { return COLS.length; }
    @Override public String getColumnName(int column) { return COLS[column]; }
    @Override public Object getValueAt(int row, int col) {
      switch (col){
        case 0:
          return sourceResults.get(row).getDisplayName();
        case 1:
          return sourceResults.get(row).getRowsInserted();
      }
      return sourceResults.get(row);
    }
    @Override public boolean isCellEditable(int row, int col) { return false; }
  }

  private static class StatusCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int col) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
//      if (value instanceof SourceStatus) {
//        SourceStatus status = (SourceStatus) value;
      int modelRow = table.convertRowIndexToModel(row);
      SourceDataModel model = (SourceDataModel) table.getModel();
      SourceResult sr = model.sourceResults.get(modelRow);

      SourceStatus status = sr.getStatus();
      setText(sr.getDisplayName());

        setIcon(getStatusIcon(status));
//      }
      return c;
    }

    private Icon getStatusIcon(SourceStatus status) {
      switch (status) {
        case SUCCESS: return AllIcons.Actions.Checked;
        case FAILED: return AllIcons.Ide.FatalError;
        case RUNNING: return AllIcons.Process.Step_1;
        default: return AllIcons.General.Gear;
      }
    }
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


}
