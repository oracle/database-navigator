package com.dbn.vector.ui.chunk;

import com.dbn.common.color.Colors;
import com.dbn.common.text.TextContent;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Fonts;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.model.resultSet.ResultSetDataModel;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.chunk.ChunkConfig;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBTextArea;
import com.intellij.util.ui.AsyncProcessIcon;
import org.jetbrains.annotations.Nullable;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import java.awt.BorderLayout;
import java.sql.ResultSet;

public class ChunkEditorForm extends DBNFormBase {

  private JPanel mainPanel;
  private JBTextArea inputTextArea;
  private ResultSetTable chunkDataTable;
  private JButton testButton;
  private DBNScrollPane outputScrollPane;
  private JScrollPane inputScrollPane;
  private JComboBox<String> chunkByComboBox;
  private JSpinner maxSpinner;
  private JComboBox<String> splitByComboBox;
  private JSpinner overlapSpinner;
  private JPanel spinPanel;
  private JPanel inputPanel;
  private JPanel outputPanel;
  private JPanel hintPanel;
  private final ConnectionRef connection;

  public ChunkEditorForm(@Nullable Disposable parent, ConnectionHandler connection, ChunkConfig config) {
    super(parent, connection.getProject());
    this.connection = connection.ref();

    initHintPanel();
    initOutputPanel();
    initConfigFields(config);
    initInputTextArea();
    initSpinner();
    initTestButton();
  }

  private void initHintPanel() {
    TextContent textContent = TextContent.plain(
            "Use this tool to experiment with different chunking settings before applying them in embedding and retrieval workflows. " +
                 "Adjust the parameters, preview the resulting chunks, and fine-tune the configuration that works best for your data.");
    DBNHintForm hintForm = new DBNHintForm(this, textContent, null, true);
    hintPanel.add(hintForm.getComponent());
  }

  private void initOutputPanel() {
    ConnectionHandler connection = getConnection();
    RecordViewInfo recordViewInfo = new RecordViewInfo("Chunk data", null);
    ResultSetDataModel dataModel = new ResultSetDataModel<>(connection);
    chunkDataTable = new ResultSetTable<>(this, dataModel, true, recordViewInfo);
    outputScrollPane.setViewportView(chunkDataTable);
    chunkDataTable.installValuePopupAddon();
    outputPanel.setBorder(Borders.lineBorder(Colors.getOutlineColor()));
  }

  private void initInputTextArea() {
    inputTextArea.getEmptyText().appendLine("Enter your sample text for chunking here");
    inputTextArea.setBackground(chunkDataTable.getBackground());
    inputTextArea.setFont(Fonts.regular());
  }

  private void initSpinner() {
    spinPanel.add(new AsyncProcessIcon("Loading"), BorderLayout.CENTER);
    spinPanel.setVisible(false);
  }

  private ConnectionHandler getConnection() {
    return connection.ensure();
  }

  private void initConfigFields(ChunkConfig chunkConfig) {
    chunkByComboBox.setSelectedItem(chunkConfig.getChunkBy());
    splitByComboBox.setSelectedItem(chunkConfig.getSplitBy());
    maxSpinner.setValue(chunkConfig.getMaxSize());
    overlapSpinner.setValue(chunkConfig.getOverlap());
  }

  private void initTestButton() {
    testButton.addActionListener(e -> {
      Dispatch.async(mainPanel,
              () -> chunkTextContent(),
              d -> applyChunkResult(d));
    });
  }

  private ResultSetDataModel chunkTextContent() {
    startActivityNotifier();
    // todo recheck
    String query = inputTextArea.getText().replace("'", ""); // TODO prepared statement param binding

    ChunkConfig configuration = getChunkConfiguration();
    ConnectionHandler connection = getConnection();
    Project project = connection.getProject();

    try {
      DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(project);
      ResultSet resultSet = vectorManager.chunkTextContent(connection, configuration, query);
      ResultSetDataModel dataModel = new ResultSetDataModel((DBNResultSet) resultSet, connection, -1);
      dataModel.fetchNextRecords(1000, false);
      return dataModel;
    } catch (Exception e) {
      Messages.showErrorDialog(project, "Failed to chunk data", e);
      return new ResultSetDataModel(connection);
    } finally {
      stopActivityNotifier();
    }
  }

  private void applyChunkResult(ResultSetDataModel chunkData){
    chunkDataTable.setModel(chunkData);
  }

  private void startActivityNotifier() {
    spinPanel.setVisible(true);
    testButton.setEnabled(false);
  }

  /**
   * Stops the spining wheel
   */
  private void stopActivityNotifier() {
    spinPanel.setVisible(false);
    testButton.setEnabled(true);
  }

  public ChunkConfig getChunkConfiguration() {
    String by = (String) chunkByComboBox.getSelectedItem();
    int max = (int) maxSpinner.getValue();
    String splitBy = (String) splitByComboBox.getSelectedItem();
    int overlap = (int) overlapSpinner.getValue();

    return new ChunkConfig(by, max, splitBy, overlap);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}
