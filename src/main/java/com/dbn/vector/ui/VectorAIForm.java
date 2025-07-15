package com.dbn.vector.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.panel.DBNCollapsiblePanel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.ui.source.ui.SourceDataForm;

import javax.swing.*;

import java.awt.*;

public class VectorAIForm extends DBNFormBase {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private JPanel chunkConfigPanel;
  private JPanel embedConfigPanel;
  private JPanel saveDataPanel;
  private JPanel hintPanel;
  private JPanel headerPanel;

  private SourceDataForm sourceDataForm;
  private ChunkConfigForm chunkConfigForm;
  private EmbedConfigForm embedConfigForm;
  private SaveVectorsForm saveVectorsForm;

  private ConnectionHandler connectionHandler;


  public VectorAIForm(ConnectionHandler connection) {
    super(connection, connection.getProject());
    this.connectionHandler = connection;
    System.out.println("fkaravvvaaaaweeeeaenkjhf");
    sourceDataForm = new SourceDataForm(this,connection);
    chunkConfigForm = new ChunkConfigForm(this,connection);
    embedConfigForm = new EmbedConfigForm(this,connection);
    saveVectorsForm = new SaveVectorsForm(this);

    System.out.println("faa");
    DBNCollapsiblePanel sourceCollapsiblePanel = new DBNCollapsiblePanel(this,sourceDataForm,true);
    sourceCollapsiblePanel.setExpanded(true);
    dataPanel.add(sourceCollapsiblePanel.getComponent());

    DBNCollapsiblePanel chunkCollapsiblePanel = new DBNCollapsiblePanel(this,chunkConfigForm,false);
    chunkCollapsiblePanel.setExpanded(false);
    chunkConfigPanel.add(chunkCollapsiblePanel.getComponent());

    DBNCollapsiblePanel embedCollapsiblePanel = new DBNCollapsiblePanel(this,embedConfigForm,false);
    embedCollapsiblePanel.setExpanded(false);
    embedConfigPanel.add(embedCollapsiblePanel.getComponent());

    DBNCollapsiblePanel saveCollapsiblePanel = new DBNCollapsiblePanel(this,saveVectorsForm,false);
    saveCollapsiblePanel.setExpanded(false);
    saveDataPanel.add(saveCollapsiblePanel.getComponent());

    initHintPanel();
    initHeaderPanel();
  }

  private void initHeaderPanel() {

    ConnectionHandler connection = connectionHandler;
    DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
    headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
  }

  private void initHintPanel() {

    TextContent hintText = TextContent.html(
            "<html>" +
                    "<body>" +
                    "<p>" +
                    "Turn Oracle tables or local files into vector embeddings — ready for semantic search, " +
                    "“chat-with-your-data,” and RAG workflows in minutes." +
                    "</p>" +
                    "</body>" +
                    "</html>"
    );
    DBNHintForm hintForm = new DBNHintForm(null, hintText, null, true);

    JComponent hintComponent = hintForm.getComponent();
    hintPanel.add(hintComponent);
  }


  @Override
  protected JComponent getMainComponent() {
//    DBNScrollPane scrollPane = new DBNScrollPane(mainPanel);
    return mainPanel;
  }
}