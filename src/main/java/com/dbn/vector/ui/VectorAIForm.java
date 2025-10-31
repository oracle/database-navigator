package com.dbn.vector.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.ui.panel.DBNCollapsiblePanel;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.ui.chunk.ChunkConfigForm;
import com.dbn.vector.ui.embed.EmbedConfigForm;
import com.dbn.vector.ui.source.ui.SourceDataForm;
import com.dbn.vector.ui.store.SaveVectorsForm;
import com.intellij.openapi.Disposable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.common.ui.alignment.FieldAligner.alignFormFields;

public class VectorAIForm extends VectorToolboxFormBase {
  private JPanel mainPanel;
  private JPanel dataPanel;
  private JPanel chunkConfigPanel;
  private JPanel embedConfigPanel;
  private JPanel saveDataPanel;
  private JPanel hintPanel;
  private JPanel headerPanel;
  private JPanel hyperlinkPanel;

  private SourceDataForm sourceDataForm;
  private ChunkConfigForm chunkConfigForm;
  private EmbedConfigForm embedConfigForm;
  private SaveVectorsForm saveVectorsForm;

  public VectorAIForm(Disposable parent, ConnectionHandler connection) {
    super(parent, connection);

    initHeaderPanel();
    initHintPanel();
    initPoweredByPanel();
    initForms();
//    initButtonListners();
    resetFormChanges();
    alignFormFields(this);
  }

  private void initForms() {
    ConnectionHandler connection = getConnection();

    sourceDataForm = new SourceDataForm(this,connection);
    DBNCollapsiblePanel sourceCollapsiblePanel = new DBNCollapsiblePanel(this,sourceDataForm,true);
    sourceCollapsiblePanel.setExpanded(true);
    dataPanel.add(sourceCollapsiblePanel.getComponent());

    chunkConfigForm = new ChunkConfigForm(this,connection);
    DBNCollapsiblePanel chunkCollapsiblePanel = new DBNCollapsiblePanel(this,chunkConfigForm,true);
    chunkCollapsiblePanel.setExpanded(true);
    chunkConfigPanel.add(chunkCollapsiblePanel.getComponent());

    embedConfigForm = new EmbedConfigForm(this,connection);
    DBNCollapsiblePanel embedCollapsiblePanel = new DBNCollapsiblePanel(this,embedConfigForm,true);
    embedCollapsiblePanel.setExpanded(true);
    embedConfigPanel.add(embedCollapsiblePanel.getComponent());

    saveVectorsForm = new SaveVectorsForm(this,connection);
    DBNCollapsiblePanel saveCollapsiblePanel = new DBNCollapsiblePanel(this,saveVectorsForm,true);
    saveCollapsiblePanel.setExpanded(true);
    saveDataPanel.add(saveCollapsiblePanel.getComponent());
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerForms(
            sourceDataForm,
            chunkConfigForm,
            embedConfigForm,
            saveVectorsForm);
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

  public VectorEmbeddingRequest getEmbeddingRequest() {
    DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(getProject());
    return vectorManager.getEmbeddingRequest(getConnectionId());
  }

  @Override
  public void resetFormChanges() {
    sourceDataForm.resetFormChanges();
    chunkConfigForm.resetFormChanges();
    embedConfigForm.resetFormChanges();
    saveVectorsForm.resetFormChanges();
  }

  @Override
  public void applyFormChanges() {
    sourceDataForm.applyFormChanges();
    chunkConfigForm.applyFormChanges();
    embedConfigForm.applyFormChanges();
    saveVectorsForm.applyFormChanges();
  }

  protected void reset() {
    SchemaId userSchema = getConnection().getUserSchema();
    getEmbeddingRequest().resetHard(userSchema);
    resetFormChanges();
  }

  private void initHeaderPanel() {
    DBNHeaderForm headerForm = new DBNHeaderForm(this, getConnection());
    headerPanel.add(headerForm.getComponent());
  }

  private void initHintPanel() {

    TextContent hintText = TextContent.plain(
            "Vector Chain Embeddings Configuration\n\n" +
                    "Use this interface to generate dense vector representations of your data using Oracle DBMS_VECTOR_CHAIN. " +
                    "Choose data from existing tables or upload file contents, customize chunking parameters, " +
                    "select from a range of pre-trained embedding models hosted in the database or configure third-party alternatives, " +
                    "and decide whether to store generated embeddings in an existing table or create a new one.\n\n" +
                    "These embeddings can be used to power Retrieval-Augmented Generation (RAG) workflows, among other applications.");
    DBNHintForm hintForm = new DBNHintForm(null, hintText, null, true);

    JComponent hintComponent = hintForm.getComponent();
    hintPanel.add(hintComponent);
  }

  private void initPoweredByPanel() {
    HyperLinkForm hyperLinkForm = HyperLinkForm.create(
            "Powered by",
            "Oracle AI Vector Search",
            "https://docs.oracle.com/en/database/oracle/oracle-database/26/vecse/overview-ai-vector-search.html");

    hyperLinkForm.setTooltipText("https://docs.oracle.com/en/database/oracle/oracle-database/26/vecse/overview-ai-vector-search.html");
    hyperlinkPanel.add(hyperLinkForm.getComponent(), BorderLayout.EAST);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}