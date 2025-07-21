package com.dbn.vector.ui.chunk;

import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Dialogs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.chunk.ChunkConfiguration;
import com.dbn.vector.model.sourceconfig.DBTableSourceConfig;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class ChunkConfigForm extends DBNFormBase implements DBNCollapsibleForm {
  private JPanel mainPanel;
  private JPanel chunkConfigPanel;
  private JComboBox<String> BYComboBox;
  private JSpinner MAXSpinner;
  private JComboBox<String> SPLITBYComboBox;
  private JSpinner OVERLAPSpinner;
  private JButton chunkLaboButton;

  public ChunkConfigForm(@Nullable Disposable parent, ConnectionHandler connectionHandler) {
    super(parent);
    System.out.println("fsakhj hjkdshkj hkjhw23qqgffkjhrftreffaaafjfjkhadfjkh jkht kjehkjthr afdff");
    initComponents();
    chunkLaboButton.addActionListener(e -> {
      ChunkConfiguration chunkConfiguration = new ChunkConfiguration(
              BYComboBox.getSelectedItem().toString(),
              (Integer) MAXSpinner.getValue(),
              (String) SPLITBYComboBox.getSelectedItem(),
              (Integer) OVERLAPSpinner.getValue()
      );
      ChunkEditorDialog dialog = new ChunkEditorDialog(getProject(),"Chunk Editor",true,connectionHandler,chunkConfiguration);
      Dialogs.show(()->dialog);

      updateChunkConfig(dialog.getChunkConfiguration());
    });
  }

  private void updateChunkConfig(ChunkConfiguration chunkConfiguration) {
    BYComboBox.setSelectedItem(chunkConfiguration.getBy());
    MAXSpinner.setValue(chunkConfiguration.getMax());
    SPLITBYComboBox.setSelectedItem(chunkConfiguration.getSplitBy());
    OVERLAPSpinner.setValue(chunkConfiguration.getOverlap());
  }

  private void initComponents() {
    MAXSpinner.setValue(300);
    OVERLAPSpinner.setValue(30);
  }

  public ChunkConfiguration getChunkConfig() {
    ChunkConfiguration chunkConfig = new ChunkConfiguration();
    chunkConfig.setBy(BYComboBox.getSelectedItem().toString());
    chunkConfig.setSplitBy(SPLITBYComboBox.getSelectedItem().toString());
    chunkConfig.setOverlap((Integer) OVERLAPSpinner.getValue());
    chunkConfig.setMax((Integer) MAXSpinner.getValue());
    return chunkConfig;
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }

  @Override
  public String getCollapsedTitle() {
    return "Chunk Config";
  }

  @Override
  public String getCollapsedTitleDetail() {
    return "";
  }

  @Override
  public String getExpandedTitle() {
    return "Chunk Config";
  }
}
