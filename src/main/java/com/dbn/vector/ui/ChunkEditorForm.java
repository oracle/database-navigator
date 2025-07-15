package com.dbn.vector.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.table.DBNTableWithGutter;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.vector.model.ChunkConfiguration;
import com.dbn.vector.model.ChunkDataModel;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextArea;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

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
  private JComboBox BYComboBox;
  private JSpinner MAXSpinner;
  private JComboBox SPLITBYComboBox;
  private JSpinner OVERLAPSpinner;
  private JCheckBox PUNCTUATIONCheckBox;
  private JCheckBox WHITESPACECheckBox;
  private JCheckBox WIDECHARCheckBox;
  private JCheckBox ALLCheckBox;
  private ChunkDataModel chunkDataModel;

  public ChunkEditorForm(@Nullable Disposable parent, @Nullable Project project, ConnectionHandler connectionHandler) {
    super(parent, project);
    this.connectionHandler = connectionHandler;
    chunkDataModel = new ChunkDataModel(connectionHandler);
    table1 = new DBNTableWithGutter<>(this, chunkDataModel, true);
    scrollPane.setViewportView(table1);

    contentTextArea.getEmptyText().appendLine("put your text to be chunked ");



    contentTextArea.setBorder(
        BorderFactory.createLineBorder(com.intellij.ui.JBColor.GRAY)
    );
    textAreatScrolPane.setBorder(BorderFactory.createEmptyBorder());

    initTestButtonListner();
  }

  private void initTestButtonListner() {
    testButton.addActionListener(e -> {
      ChunkConfiguration chunkConfiguration = getChunkConfiguration();
      String text = contentTextArea.getText();
      SchemaId schemaId = connectionHandler.getUserSchema();

      try {
        DatabaseInterfaceInvoker.execute(HIGHEST,
                "",
                "",
                connectionHandler.getProject(),
                connectionHandler.getConnectionId(),
                schemaId,
                conn -> {
                  chunkDataModel.refresh(chunkConfiguration,text,conn);
                });
      } catch (SQLException ex) {
        throw new RuntimeException(ex);
      }
//      Background.run(()->);

    });
  }

  private ChunkConfiguration getChunkConfiguration() {
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
