package com.dbn.vector.ui.chunk;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.table.DBNTableWithGutter;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.vector.model.chunk.ChunkConfiguration;
import com.dbn.vector.model.ChunkDataModel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import java.awt.*;
import java.sql.SQLException;

import static com.dbn.common.Priority.HIGHEST;

public class ChunkEditorForm extends DBNFormBase {

  private final ConnectionHandler connectionHandler;
  private JPanel mainPanel;
  private JBTextArea contentTextArea;
  private JTable table1;
  private JButton testButton;
  private DBNScrollPane scrollPane;
  private JScrollPane textAreatScrolPane;
  private JComboBox<String> BYComboBox;
  private JSpinner MAXSpinner;
  private JComboBox<String> SPLITBYComboBox;
  private JSpinner OVERLAPSpinner;
  private JPanel spinPanel;
  private ChunkDataModel chunkDataModel;

  public ChunkEditorForm(@Nullable Disposable parent, @Nullable Project project, ConnectionHandler connectionHandler, ChunkConfiguration chunkConfiguration) {
    super(parent, project);
    this.connectionHandler = connectionHandler;
    chunkDataModel = new ChunkDataModel(connectionHandler);
    table1 = new DBNTableWithGutter<>(this, chunkDataModel, true);
    scrollPane.setViewportView(table1);
    spinPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
    System.out.println("f");
    contentTextArea.getEmptyText().appendLine("Put your text to be chunked ");



    contentTextArea.setBorder(
        BorderFactory.createLineBorder(com.intellij.ui.JBColor.GRAY)
    );
    textAreatScrolPane.setBorder(BorderFactory.createEmptyBorder());
    fillConfig(chunkConfiguration);
    initTestButtonListner();
  }

  private void fillConfig(ChunkConfiguration chunkConfiguration) {
    BYComboBox.setSelectedItem(chunkConfiguration.getBy());
    MAXSpinner.setValue(chunkConfiguration.getMax());
    SPLITBYComboBox.setSelectedItem(chunkConfiguration.getSplitBy());
    OVERLAPSpinner.setValue(chunkConfiguration.getOverlap());
  }

  private void initTestButtonListner() {
    testButton.addActionListener(e -> {
      ChunkConfiguration chunkConfiguration = getChunkConfiguration();
      String query = contentTextArea.getText();
      String preparedQuery = query.replace("'", "");
      SchemaId schemaId = connectionHandler.getUserSchema();

      try {
        DatabaseInterfaceInvoker.execute(HIGHEST,
                "",
                "",
                connectionHandler.getProject(),
                connectionHandler.getConnectionId(),
                schemaId,
                conn -> {
                  try {
                    startActivityNotifier();
                    chunkDataModel.refresh(chunkConfiguration,preparedQuery,conn);
                  }finally {
                    stopActivityNotifier();
                  }

                });
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
//      Background.run(()->);

    });
  }

  private void startActivityNotifier() {
    spinPanel.setVisible(true);
  }

  /**
   * Stops the spining wheel
   */
  private void stopActivityNotifier() {
    spinPanel.setVisible(false);
  }

  public ChunkConfiguration getChunkConfiguration() {
    String by = (String) BYComboBox.getSelectedItem();
    int max = (int) MAXSpinner.getValue();
    String splitBy = (String) SPLITBYComboBox.getSelectedItem();
    int overlap = (int) OVERLAPSpinner.getValue();

    return new ChunkConfiguration(by, max, splitBy, overlap);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
