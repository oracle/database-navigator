package com.dbn.vector.result;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.text.HiddenCaret;
import com.dbn.common.ui.util.Fonts;
import com.dbn.vector.model.StepResult;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class PipelineStepForm extends DBNFormBase  {
  private JPanel mainPanel;
  private JLabel titleLabel;
  private JTextPane descriptionTextArea;
  private JTextPane reasonTextArea;
  private JPanel statusPanel;
  private JLabel statusLabel;

  private final StepResult stepResult;

  public PipelineStepForm(@Nullable VectorEmbeddingExecutionResultForm parent, StepResult stepResult) {
    super(parent);
    this.stepResult = stepResult;
    Color greyContent = Colors.faded(UIUtil.getLabelForeground());
    Font largerFont = Fonts.regular(1);

    titleLabel.setText(stepResult.getStep().getDisplayName());
    titleLabel.setFont(largerFont);
    statusLabel.setForeground(greyContent);
    descriptionTextArea.setFont(JBUI.Fonts.label());

    descriptionTextArea.setText(stepResult.getStep().getDescription());
    descriptionTextArea.setCaret(new HiddenCaret());

    if (!stepResult.isOk()){
      reasonTextArea.setText(stepResult.getErrorCode()+stepResult.getErrorMessage());
      reasonTextArea.setFont(JBUI.Fonts.label());
      reasonTextArea.setForeground(greyContent);
      reasonTextArea.setCaret(new HiddenCaret());
    }
    updateStepStatus();


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
