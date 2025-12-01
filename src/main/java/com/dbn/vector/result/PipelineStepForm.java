package com.dbn.vector.result;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.link.DBNHyperlinkLabel;
import com.dbn.common.ui.text.HiddenCaret;
import com.dbn.common.ui.util.Fonts;
import com.dbn.connection.ConnectionHandler;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.editor.data.DatasetEditorManager;
import com.dbn.editor.data.filter.*;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.vector.model.StepResult;
import com.dbn.vector.model.SourceResult;
import com.dbn.vector.model.PipelineStep;
import com.dbn.vector.model.TableResult;
import com.dbn.vector.model.FileResult;
import com.intellij.icons.AllIcons;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Color;
import java.awt.Font;

public class PipelineStepForm extends DBNFormBase  {
  private JPanel mainPanel;
  private JLabel titleLabel;
//  private JTextPane descriptionTextArea;
  private JTextPane reasonTextArea;
  private JPanel statusPanel;
  private JLabel statusLabel;
  private JLabel durationValueLabel;
  private DBNInfoLabel infoLabel;
  private DBNHyperlinkLabel linkLabel;

  private final StepResult stepResult;

  public PipelineStepForm(@Nullable VectorEmbeddingExecutionResultForm parent, StepResult stepResult, int order) {
    super(parent);
    this.stepResult = stepResult;
    Color greyContent = Colors.faded(UIUtil.getLabelForeground());
    Color redContent = UIUtil.getErrorForeground();
    Font largerFont = Fonts.regular(1);
    titleLabel.setText(order+" "+stepResult.getStep().getDisplayName());
    titleLabel.setFont(largerFont);
    statusLabel.setForeground(greyContent);
    initInfoLabel();



    linkLabel.setHyperlinkText(stepResult.getLink());
    linkLabel.setIcon(stepResult.getIcon());

    linkLabel.addHyperlinkListener(e -> {
      VectorEmbeddingExecutionResultForm parentForm = getParentComponent();
      if (parentForm == null) return;

      ConnectionHandler connection = parentForm.getResult().getConnection();
      String[] parts = stepResult.getLink().split("\\.");
      if (parts.length != 2) return;

      String schemaName = parts[0];
      String tableName = parts[1];

      DBSchema schema = connection.getSchema(connection.getSchemaId(schemaName));
      if (schema == null) return;

      DBTable table = schema.getTable(tableName);
      if (table == null) return;

      // Get the currently selected source
      SourceResult selectedSource = parentForm.getSelectedSource();
      DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(connection.getProject());
      boolean editorAlreadyOpen = editorManager.isFileOpen(table);

      // Only apply filter for the embedding destination table (ENSURE_DESTINATION step)
      if (stepResult.getStep() == PipelineStep.ENSURE_DESTINATION && selectedSource != null) {
        createAndApplyFilter(connection, table, selectedSource);
      }

      editorManager.connectAndOpenEditor(table, null, true, true);

      if (editorAlreadyOpen) {
        DatasetEditorManager datasetEditorManager = DatasetEditorManager.getInstance(connection.getProject());
        datasetEditorManager.reloadEditorData(table);
      }
    });






    durationValueLabel.setForeground(greyContent);
    durationValueLabel.setText(String.format("%.1f",(double)stepResult.getDuration()/1000)+"s");
    if (!stepResult.isOk()){
      reasonTextArea.setText(stepResult.getErrorCode()+stepResult.getErrorMessage());
      reasonTextArea.setFont(JBUI.Fonts.label());
      reasonTextArea.setForeground(redContent);
      reasonTextArea.setCaret(new HiddenCaret());
    }
    reasonTextArea.setVisible(!stepResult.isOk());
    linkLabel.setVisible(!stepResult.getLink().isEmpty());
    updateStepStatus();


  }

  private void createAndApplyFilter(ConnectionHandler connection, DBTable table, SourceResult sourceResult) {
    DatasetFilterManager filterManager = DatasetFilterManager.getInstance(connection.getProject());
    DatasetFilterGroup filterGroup = filterManager.getFilterGroup(table);

    // Create temporary custom filter
    DatasetCustomFilter embeddingFilter = new DatasetCustomFilter(
            filterGroup,
            "New Embeddings - " + sourceResult.getName()
    );
    embeddingFilter.setTemporary(true);

    String whereClause;

    if (sourceResult instanceof TableResult) {
      TableResult tableResult = (TableResult) sourceResult;

      String[] parts = tableResult.getIdentifier().split("\\.");

      if (parts.length != 2) return;

      String tableName = parts[1];

      whereClause = String.format(
              "JSON_VALUE(metadata, '$.embedding_source.table_name') = '%s'",
              tableName.replace("'", "''")
      );
    } else if (sourceResult instanceof FileResult) {
      FileResult fileResult = (FileResult) sourceResult;
      String fileId = fileResult.getDocId();

      whereClause = String.format(
              "JSON_VALUE(metadata, '$.embedding_source.primary_key') = '%s'",
              fileId.replace("'", "''")
      );
    } else {
      return;
    }

    embeddingFilter.setCondition(whereClause);
    filterManager.setActiveFilter(table, embeddingFilter);
  }

  private void initInfoLabel() {
    infoLabel.setContent(TextContent.plain(stepResult.getStep().getDescription()));
  }

  private void updateStepStatus() {
    StepResult.STEP_STATUS status = stepResult.getStatus();
    Icon icon = null;
    if (status == StepResult.STEP_STATUS.FAILED) {
      statusLabel.setText("Not OK");
      icon = Icons.COMMON_STATUS_ERROR;
      statusLabel.setIcon(icon);
    }else if (status == StepResult.STEP_STATUS.SUCCEEDED){
      statusLabel.setText("OK");
      icon = Icons.COMMON_STATUS_SUCCESS;
      statusLabel.setIcon(icon);
    }else if (status == StepResult.STEP_STATUS.NOT_STARTED){
      statusLabel.setText("Not Started");
      icon = AllIcons.Process.Step_passive;
      statusLabel.setIcon(icon);
    }

  }


  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
