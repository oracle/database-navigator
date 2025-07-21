package com.dbn.vector.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.chunk.ChunkConfiguration;
import com.dbn.vector.model.embed.EmbedConfig;
import com.dbn.vector.model.sourceconfig.SourceConfig;
import com.dbn.vector.model.store.StoreConfig;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

public class VectorAiDialog extends DBNDialog<VectorAIForm> {
  ConnectionHandler connectionHandler;

  public VectorAiDialog(@Nullable Project project, String title, boolean canBeParent, ConnectionHandler connection) {
    super(project, title, canBeParent);
    connectionHandler = connection;
    setDefaultSize(600, 1000);
    hideAction(getOKAction());
    renameAction(getCancelAction(),"Close");
    init();

  }

  @Override
  protected @NotNull VectorAIForm createForm() {
    return new VectorAIForm(connectionHandler);
  }

//  @Override
//  protected void doOKAction() {
//    VectorAIForm form = getForm();
//
//    SourceConfig sourceConfig = form.getSourceDataForm().getSourceConfig();
//    ChunkConfiguration chunkConfiguration = form.getChunkConfigForm().getChunkConfig();
//    EmbedConfig embedConfig = form.getEmbedConfigForm().getEmbedConfig();
//    StoreConfig storeConfig = form.getSaveVectorsForm().getStoreConfig();
//
//    try {
//      DatabaseVectorManager.getInstance(getProject()).query(sourceConfig,chunkConfiguration,embedConfig,storeConfig,connectionHandler);
//    } catch (SQLException e) {
//      throw new RuntimeException(e);
//    }
//  }
}