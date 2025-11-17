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
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.vector.model.StepResult;
import com.dbn.vfs.DatabaseFileSystem;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.fileEditor.FileEditorManager;
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
  private JPanel linkIcon;

  private final StepResult stepResult;

  public PipelineStepForm(@Nullable VectorEmbeddingExecutionResultForm parent, StepResult stepResult) {
    super(parent);
    this.stepResult = stepResult;
    Color greyContent = Colors.faded(UIUtil.getLabelForeground());
    Color redContent = UIUtil.getErrorForeground();
    Font largerFont = Fonts.regular(1);
    System.out.println("fhjfhg");
    titleLabel.setText(stepResult.getStep().getDisplayName());
    titleLabel.setFont(largerFont);
    statusLabel.setForeground(greyContent);
    initInfoLabel();
//    linkLabel.setFont(JBUI.Fonts.label());
    linkLabel.setHyperlinkText(stepResult.getLink());
    linkIcon.add(new JLabel(stepResult.getIcon()));

    linkLabel.addHyperlinkListener(e->{
      ConnectionHandler connection = parent.getResult().getConnection();
      String schemaName = stepResult.getLink().split("\\.")[0];
      String tableName = stepResult.getLink().split("\\.")[1];

      DBSchema schema = connection.getSchema(connection.getSchemaId(schemaName));
      DBTable table = schema.getTable(tableName);

      DatabaseFileSystem fileSystem = DatabaseFileSystem.getInstance();
      DBEditableObjectVirtualFile databaseFile = fileSystem.findOrCreateDatabaseFile(table);

      FileEditorManager fileEditorManager = FileEditorManager.getInstance(getProject());
      fileEditorManager.openFile(databaseFile, true);
    });



    durationValueLabel.setForeground(greyContent);
    durationValueLabel.setText((double)stepResult.getDuration()/1000+"s");
    if (!stepResult.isOk()){
      reasonTextArea.setText(stepResult.getErrorCode()+stepResult.getErrorMessage());
      reasonTextArea.setFont(JBUI.Fonts.label());
      reasonTextArea.setForeground(redContent);
      reasonTextArea.setCaret(new HiddenCaret());
    }
    reasonTextArea.setVisible(!stepResult.isOk());
    updateStepStatus();


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
    }else if (status == StepResult.STEP_STATUS.SUCCEEDED){
      statusLabel.setText("OK");
      icon = Icons.COMMON_STATUS_SUCCESS;
    }else if (status == StepResult.STEP_STATUS.NOT_STARTED){
      statusLabel.setText("Not Started");
      icon = AllIcons.Process.Step_passive;
    }

    statusPanel.add(new JLabel(icon));
  }


  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
