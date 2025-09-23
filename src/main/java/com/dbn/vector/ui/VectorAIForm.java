package com.dbn.vector.ui;

import com.dbn.common.routine.Consumer;
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.panel.DBNCollapsiblePanel;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.chunk.ChunkConfiguration;
import com.dbn.vector.model.embed.EmbedConfig;
import com.dbn.vector.model.sourceconfig.SourceConfig;
import com.dbn.vector.model.store.StoreConfig;
import com.dbn.vector.ui.chunk.ChunkConfigForm;
import com.dbn.vector.ui.embed.EmbedConfigForm;
import com.dbn.vector.ui.source.ui.SourceDataForm;
import com.dbn.vector.ui.store.SaveVectorsForm;
import com.intellij.openapi.Disposable;

import javax.swing.*;

import java.awt.*;
import java.sql.SQLException;

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


  public VectorAIForm(Disposable parent, ConnectionHandler connection) {
    super(parent, connection.getProject());
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

    DBNCollapsiblePanel chunkCollapsiblePanel = new DBNCollapsiblePanel(this,chunkConfigForm,true);
    chunkCollapsiblePanel.setExpanded(true);
    chunkConfigPanel.add(chunkCollapsiblePanel.getComponent());

    DBNCollapsiblePanel embedCollapsiblePanel = new DBNCollapsiblePanel(this,embedConfigForm,true);
    embedCollapsiblePanel.setExpanded(true);
    embedConfigPanel.add(embedCollapsiblePanel.getComponent());

    DBNCollapsiblePanel saveCollapsiblePanel = new DBNCollapsiblePanel(this,saveVectorsForm,true);
    saveCollapsiblePanel.setExpanded(true);
    saveDataPanel.add(saveCollapsiblePanel.getComponent());

    initHintPanel();
    initHeaderPanel();
//    initButtonListners();
  }

//  private void initButtonListners() {
//    ApplyButton.addActionListener(e -> {
//
//      SourceConfig sourceConfig = this.getSourceDataForm().getSourceConfig();
//      ChunkConfiguration chunkConfiguration = this.getChunkConfigForm().getChunkConfig();
//      EmbedConfig embedConfig = this.getEmbedConfigForm().getEmbedConfig();
//      StoreConfig storeConfig = this.getSaveVectorsForm().getStoreConfig();
//
//      try {
//        Runnable callbackInfo = ()->{
//          Messages.showInfoDialog(getProject(), "Embedding Succeeded ","Your data has been embedded successfully!");
//          chatNowButton.setEnabled(true);
//        };
//        Consumer<Exception> callbackError = (ex) -> {
//          Messages.showErrorDialog(getProject(), "Embedding Failed", ex.getMessage(), ex);
//        };
//        DatabaseVectorManager.getInstance(getProject()).query(sourceConfig,chunkConfiguration,embedConfig,storeConfig,connectionHandler,callbackInfo,callbackError);
//      } catch (SQLException ex) {
//        Messages.showErrorDialog(getProject(), null,
//                ex.getMessage(),ex);
//        throw new RuntimeException(ex);
//      }
//    });
//
//    chatNowButton.addActionListener(e -> {
//      //todo open select ai
//
//    });
//  }

  private void initHeaderPanel() {

    ConnectionHandler connection = connectionHandler;
    DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
    headerPanel.add(headerForm.getComponent());
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

  public SourceDataForm getSourceDataForm() {
    return sourceDataForm;
  }

  public ChunkConfigForm getChunkConfigForm() {
    return chunkConfigForm;
  }

  public EmbedConfigForm getEmbedConfigForm() {
    return embedConfigForm;
  }

  public SaveVectorsForm getSaveVectorsForm() {
    return saveVectorsForm;
  }

  @Override
  protected JComponent getMainComponent() {
//    DBNScrollPane scrollPane = new DBNScrollPane(mainPanel);
    return mainPanel;
  }
}