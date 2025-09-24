package com.dbn.vector.ui;

import com.dbn.common.routine.Consumer;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.chunk.ChunkConfiguration;
import com.dbn.vector.model.embed.EmbedConfig;
import com.dbn.vector.model.sourceconfig.SourceConfig;
import com.dbn.vector.model.store.StoreConfig;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class VectorAiDialog extends DBNDialog<VectorAIForm> {
  private final ConnectionRef connection;
  public VectorAiDialog(ConnectionHandler connection) {
    super(connection.getProject(), "Vector Toolkit", true);
    this.connection = connection.ref();
    setDefaultSize(600, 1000);
//    hideAction(getOKAction());
//    renameAction(getCancelAction(),"Close");
    init();
  }

  private ConnectionHandler getConnection() {
    return connection.ensure();
  }

  @Override
  protected @NotNull VectorAIForm createForm() {
    return new VectorAIForm(this,getConnection());
  }

  @Override
  protected void doOKAction() {
    VectorAIForm form = getForm();
    SourceConfig sourceConfig = form.getSourceDataForm().getSourceConfig();
    ChunkConfiguration chunkConfiguration = form.getChunkConfigForm().getChunkConfig();
    EmbedConfig embedConfig = form.getEmbedConfigForm().getEmbedConfig();
    StoreConfig storeConfig = form.getSaveVectorsForm().getStoreConfig();

    try {
      Runnable callbackInfo = ()->{
        Messages.showInfoDialog(getProject(), "Embedding Succeeded ","Your data has been embedded successfully!");
      };
      Consumer<Exception> callbackError = (ex) -> {
        Messages.showErrorDialog(getProject(), "Embedding Failed", ex.getMessage(), ex);
      };
      DatabaseVectorManager.getInstance(getProject()).query(sourceConfig,chunkConfiguration,embedConfig,storeConfig,getConnection(),callbackInfo,callbackError);
    } catch (SQLException ex) {
      Messages.showErrorDialog(getProject(), null,
              ex.getMessage(),ex);
      throw new RuntimeException(ex);
    }
  }
}