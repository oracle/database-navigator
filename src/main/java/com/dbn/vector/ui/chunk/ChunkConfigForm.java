package com.dbn.vector.ui.chunk;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.chunk.ChunkConfig;
import com.dbn.vector.ui.VectorToolboxFormBase;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

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

    chunkLaboButton.addActionListener(e -> {
      ChunkConfig chunkConfig = getConfig().clone();
      ChunkEditorDialog dialog = new ChunkEditorDialog(getConnection(), chunkConfig);
      Dialogs.show(()->dialog);

      updateChunkConfig(dialog.getChunkConfig());
    });
  }

  private Integer getMaxSize() {
    return (Integer) maxSizeSpinner.getValue();
  }

  private Integer getOverlap() {
    return (Integer) overlapSpinner.getValue();
  }

  private void updateChunkConfig(ChunkConfig chunkConfig) {
    chunkByComboBox.setSelectedItem(chunkConfig.getChunkBy());
    splitByComboBox.setSelectedItem(chunkConfig.getSplitBy());
    maxSizeSpinner.setValue(chunkConfig.getMaxSize());
    overlapSpinner.setValue(chunkConfig.getOverlap());
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerFieldGroup(chunkByLabel/*, chunkByComboBox*/);
    alignerData.registerFieldGroup(splitByLabel/*, splitByComboBox*/);
  }

  @Override
  protected void initValidation() {
    addValidation(maxSizeSpinner, n-> {
              int max = (Integer) n.getValue();
              int overlap = getOverlap();
              String by = (String) chunkByComboBox.getSelectedItem();
              switch (by) {
                case "CHARACTERS":
                  return max > 50 && max < 4000;
                case "WORDS":
                  return max > 10 && max < 1000;
              }
              return false;
            }
            ,"Please enter a valid max");


    addValidation(overlapSpinner, o->{
              int max = getMaxSize();
              int overlap = (Integer) o.getValue();
              return overlap == 0 || (overlap>max*5/100 && overlap<max*20/100);
            }
            ,"Please enter a valid overlap: 5% to 20% of MAX");
  }

  @Override
  public void resetFormChanges() {
    ChunkConfig config = getConfig();
    setSelection(chunkByComboBox, config.getChunkBy());
    setSelection(splitByComboBox, config.getSplitBy());
    maxSizeSpinner.setValue(config.getMaxSize());
    overlapSpinner.setValue(config.getOverlap());
  }

  @Override
  public void applyFormChanges() {
    ChunkConfig config = getConfig();
    config.setChunkBy(getSelection(chunkByComboBox));
    config.setSplitBy(getSelection(splitByComboBox));
    config.setMaxSize((Integer) maxSizeSpinner.getValue());
    config.setOverlap((Integer) overlapSpinner.getValue());
  }

  public ChunkConfig getConfig() {
    return getEmbeddingRequest().getChunkConfig();
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public String getCollapsedTitle() {
    return "Chunk Configuration";
  }

  @Override
  public String getCollapsedTitleDetail() {
    return "";
  }

  @Override
  public String getExpandedTitle() {
    return "Chunk Configuration";
  }
}
