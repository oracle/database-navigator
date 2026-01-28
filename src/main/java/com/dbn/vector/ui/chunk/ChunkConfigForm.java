package com.dbn.vector.ui.chunk;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.chunk.ChunkConfig;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.vector.model.chunk.ChunkConfigValidator.validateMaxSize;
import static com.dbn.vector.model.chunk.ChunkConfigValidator.validateOverlap;

public class ChunkConfigForm extends VectorToolboxFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JComboBox<String> chunkByComboBox;
  private JComboBox<String> splitByComboBox;
  private JSpinner maxSizeSpinner;
  private JSpinner overlapSpinner;
  private JButton chunkLaboButton;
  private JLabel chunkByLabel;
  private JLabel splitByLabel;

  public ChunkConfigForm(@Nullable Disposable parent, ConnectionHandler connection) {
    super(parent, connection);

    chunkLaboButton.addActionListener(e -> openChunkLab());
  }

  private void openChunkLab() {
    ChunkConfig chunkConfig = new ChunkConfig();
    applyFormChanges(chunkConfig);

    Dialogs.show(() -> new ChunkEditorDialog(getConnection(), chunkConfig),
            (dialog, exitCode) -> when(
                    exitCode == DialogWrapper.OK_EXIT_CODE,
                    () -> resetFormChanges(dialog.getChunkConfig())));
  }

  private Integer getMaxSize() {
    return (Integer) maxSizeSpinner.getValue();
  }

  private Integer getOverlap() {
    return (Integer) overlapSpinner.getValue();
  }

  @Nullable
  private String getChunkBy() {
    return getSelection(chunkByComboBox);
  }

  @Nullable
  private String getSplitBy() {
    return getSelection(splitByComboBox);
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(chunkByLabel/*, chunkByComboBox*/);
    alignerData.registerFieldGroup(splitByLabel/*, splitByComboBox*/);
  }

  @Override
  protected void initValidation() {
    addValidation(maxSizeSpinner, n -> validateMaxSize(getChunkBy(), getMaxSize()));
    addValidation(overlapSpinner, o-> validateOverlap(getMaxSize(), getOverlap()));
  }

  @Override
  public void resetFormChanges() {
    ChunkConfig config = getConfig();
    resetFormChanges(config);
  }

  private void resetFormChanges(ChunkConfig config) {
    setSelection(chunkByComboBox, config.getChunkBy());
    setSelection(splitByComboBox, config.getSplitBy());
    maxSizeSpinner.setValue(config.getMaxSize());
    overlapSpinner.setValue(config.getOverlap());
  }

  @Override
  public void applyFormChanges() {
    ChunkConfig config = getConfig();
    applyFormChanges(config);
  }

  private void applyFormChanges(ChunkConfig config) {
    config.setChunkBy(getChunkBy());
    config.setSplitBy(getSplitBy());
    config.setMaxSize(getMaxSize());
    config.setOverlap(getOverlap());
  }

  public ChunkConfig getConfig() {
    return getEmbeddingRequest().getChunkConfig();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public String getFormTitle() {
    return "Chunk Configuration";
  }

  @Override
  public String getFormTitleDetail() {
    return getChunkBy() + " / " + getSplitBy() + " / " + maxSizeSpinner.getValue() + " / " + overlapSpinner.getValue();
  }
}
