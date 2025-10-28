package com.dbn.vector.ui.chunk;

import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.chunk.ChunkConfiguration;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;

public class ChunkConfigForm extends DBNFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JComboBox<String> chunkByComboBox;
  private JComboBox<String> splitByComboBox;
  private JSpinner maxSizeSpinner;
  private JSpinner overlapSpinner;
  private JButton chunkLaboButton;
  private JLabel chunkByLabel;
  private JLabel splitByLabel;

  public ChunkConfigForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
    initComponents();
    chunkLaboButton.addActionListener(e -> {
      ChunkConfiguration chunkConfiguration = new ChunkConfiguration(
              chunkByComboBox.getSelectedItem().toString(),
              (Integer) maxSizeSpinner.getValue(),
              (String) splitByComboBox.getSelectedItem(),
              (Integer) overlapSpinner.getValue()
      );
      ChunkEditorDialog dialog = new ChunkEditorDialog(connectionHandler,chunkConfiguration);
      Dialogs.show(()->dialog);

      updateChunkConfig(dialog.getChunkConfiguration());
    });

    initValidation();
  }

  private void updateChunkConfig(ChunkConfiguration chunkConfiguration) {
    chunkByComboBox.setSelectedItem(chunkConfiguration.getBy());
    maxSizeSpinner.setValue(chunkConfiguration.getMax());
    splitByComboBox.setSelectedItem(chunkConfiguration.getSplitBy());
    overlapSpinner.setValue(chunkConfiguration.getOverlap());
  }

  private void initComponents() {
    maxSizeSpinner.setValue(300);
    overlapSpinner.setValue(30);
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
              int overlap = (Integer) overlapSpinner.getValue();
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
              int max = (Integer) maxSizeSpinner.getValue();
              int overlap = (Integer) o.getValue();
              return overlap == 0 || (overlap>max*5/100 && overlap<max*20/100);
            }
            ,"Please enter a valid overlap: 5% to 20% of MAX");
  }

  public ChunkConfiguration getChunkConfig() {
    ChunkConfiguration chunkConfig = new ChunkConfiguration();
    chunkConfig.setBy(chunkByComboBox.getSelectedItem().toString());
    chunkConfig.setSplitBy(splitByComboBox.getSelectedItem().toString());
    chunkConfig.setOverlap((Integer) overlapSpinner.getValue());
    chunkConfig.setMax((Integer) maxSizeSpinner.getValue());
    return chunkConfig;
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
