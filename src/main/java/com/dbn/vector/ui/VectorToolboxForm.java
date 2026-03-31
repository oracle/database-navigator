package com.dbn.vector.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.alignment.FieldAlignerData;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.link.HyperLinkForm;
import com.dbn.common.ui.panel.DBNCollapsiblePanel;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.request.EmbeddingChunkingConfig;
import com.dbn.vector.model.request.EmbeddingDestinationConfig;
import com.dbn.vector.model.request.EmbeddingModelConfig;
import com.dbn.vector.model.request.EmbeddingSourceConfig;
import com.dbn.vector.model.request.EmbeddingStagingConfig;
import com.dbn.vector.ui.request.EmbeddingChunkingConfigForm;
import com.dbn.vector.ui.request.EmbeddingDestinationConfigForm;
import com.dbn.vector.ui.request.EmbeddingModelConfigForm;
import com.dbn.vector.ui.request.EmbeddingSourceConfigForm;
import com.dbn.vector.ui.request.EmbeddingStagingConfigForm;
import com.intellij.openapi.Disposable;
import lombok.Getter;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.common.text.TextContent.html;

public class VectorToolboxForm extends VectorToolboxFormBase {
  private JPanel mainPanel;
  private JPanel sourcePanel;
  private JPanel chunkConfigPanel;
  private JPanel embedConfigPanel;
  private JPanel saveDataPanel;
  private JPanel hintPanel;
  private JPanel headerPanel;
  private JPanel hyperlinkPanel;
  private JPanel stagingConfigPanel;

  private @Getter EmbeddingSourceConfigForm embeddingSourceForm;
  private @Getter EmbeddingStagingConfigForm stagingConfigForm;
  private @Getter EmbeddingChunkingConfigForm chunkingConfigForm;
  private @Getter EmbeddingModelConfigForm modelForm;
  private @Getter EmbeddingDestinationConfigForm destinationForm;

  private final VectorEmbeddingRequest request;

  public VectorToolboxForm(Disposable parent, VectorEmbeddingRequest request) {
    super(parent);
    this.request = request;

    initHeaderPanel();
    initHintPanel();
    initPoweredByPanel();
    initForms();
//    initButtonListners();
    resetFormChanges();
    updateFieldAlignment();
  }

  private void initForms() {
    EmbeddingSourceConfig sourceConfig = request.getSourceConfig();
    embeddingSourceForm = new EmbeddingSourceConfigForm(this);
    DBNCollapsiblePanel sourceCollapsiblePanel = new DBNCollapsiblePanel(this, embeddingSourceForm, true /*TODO async sourceConfig.isExpanded()*/);
    sourceCollapsiblePanel.setInfoContent(html(this, "info/embedding_source_config_info.html.ft"));
    sourceCollapsiblePanel.addToggleListener(expanded -> sourceConfig.setExpanded(expanded));
    sourcePanel.add(sourceCollapsiblePanel.getComponent());

    EmbeddingStagingConfig stagingConfig = request.getStagingConfig();
    stagingConfigForm = new EmbeddingStagingConfigForm(this);
    DBNCollapsiblePanel stagingCollapsiblePanel = new DBNCollapsiblePanel(this, stagingConfigForm, true /*TODO async stagingConfig.isExpanded()*/);
    stagingCollapsiblePanel.setInfoContent(html(this, "info/embedding_staging_config_info.html.ft"));
    stagingCollapsiblePanel.addToggleListener(expanded -> stagingConfig.setExpanded(expanded));
    stagingConfigPanel.add(stagingCollapsiblePanel.getComponent());

    EmbeddingChunkingConfig chunkConfig = request.getChunkConfig();
    chunkingConfigForm = new EmbeddingChunkingConfigForm(this);
    DBNCollapsiblePanel chunkCollapsiblePanel = new DBNCollapsiblePanel(this, chunkingConfigForm, true /*TODO async chunkConfig.isExpanded()*/);
    chunkCollapsiblePanel.setInfoContent(html(this, "info/embedding_chunking_config_info.html.ft"));
    chunkCollapsiblePanel.addToggleListener(expanded -> chunkConfig.setExpanded(expanded));
    chunkConfigPanel.add(chunkCollapsiblePanel.getComponent());

    EmbeddingModelConfig modelConfig = request.getModelConfig();
    modelForm = new EmbeddingModelConfigForm(this);
    DBNCollapsiblePanel embedCollapsiblePanel = new DBNCollapsiblePanel(this, modelForm, true /* TODO async embedConfig.isExpanded()*/);
    embedCollapsiblePanel.addToggleListener(expanded -> modelConfig.setExpanded(expanded));
    embedConfigPanel.add(embedCollapsiblePanel.getComponent());

    EmbeddingDestinationConfig destinationConfig = request.getDestinationConfig();
    destinationForm = new EmbeddingDestinationConfigForm(this);
    DBNCollapsiblePanel destinationCollapsiblePanel = new DBNCollapsiblePanel(this, destinationForm, true /*TODO async storeConfig.isExpanded()*/);
    destinationCollapsiblePanel.setInfoContent(html(this, "info/embedding_destination_config_info.html.ft"));
    destinationCollapsiblePanel.addToggleListener(expanded -> destinationConfig.setExpanded(expanded));
    saveDataPanel.add(destinationCollapsiblePanel.getComponent());
  }

  @Override
  protected void initFieldAlignment() {
    FieldAlignerData alignerData = getFieldAlignerData();
    alignerData.registerForms(
            embeddingSourceForm,
            stagingConfigForm,
            chunkingConfigForm,
            modelForm,
            destinationForm);
  }

  public void setStagingConfigVisible(boolean visible) {
    stagingConfigPanel.setVisible(visible);
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
    return request;
  }

  @Override
  protected VectorToolboxForm getToolboxForm() {
    return this;
  }

  @Override
  public void resetFormChanges() {
    embeddingSourceForm.resetFormChanges();
    stagingConfigForm.resetFormChanges();
    chunkingConfigForm.resetFormChanges();
    modelForm.resetFormChanges();
    destinationForm.resetFormChanges();
  }

  @Override
  public void applyFormChanges() {
    embeddingSourceForm.applyFormChanges();
    stagingConfigForm.applyFormChanges();
    chunkingConfigForm.applyFormChanges();
    modelForm.applyFormChanges();
    destinationForm.applyFormChanges();
  }

  public void saveRequestTemplate(boolean reset) {
    VectorEmbeddingRequest requestTemplate = request.clone();
    if (reset) requestTemplate.resetSoft();

    ConnectionId connectionId = getConnectionId();
    DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(getProject());
    vectorManager.setRequestTemplate(connectionId, requestTemplate);
  }

  protected void reset() {
    SchemaId userSchema = getConnection().getUserSchemaId();
    request.resetHard(userSchema);
    saveRequestTemplate(false);
    resetFormChanges();
  }

  private void initHeaderPanel() {
    DBNHeaderForm headerForm = new DBNHeaderForm(this, getConnection());
    headerPanel.add(headerForm.getComponent());
  }

  private void initHintPanel() {

    TextContent hintText = TextContent.plain(
            "Vector Chain Embeddings Configuration\n\n" +
                    "Use this interface to generate dense vector representations of your data using the Oracle DBMS_VECTOR and DBMS_VECTOR_CHAIN utilities. " +
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

    hyperlinkPanel.add(hyperLinkForm.getComponent(), BorderLayout.EAST);
  }

  @Override
  protected JComponent getMainComponent() {
    return mainPanel;
  }
}