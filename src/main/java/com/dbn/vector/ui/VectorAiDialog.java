package com.dbn.vector.ui;

import com.dbn.common.routine.Consumer;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.VectorEmbeddingRequest;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class VectorAiDialog extends DBNDialog<VectorAIForm> {
  private final ConnectionRef connection;
  public VectorAiDialog(ConnectionHandler connection) {
    super(connection.getProject(), "Vector Toolbox", true);
    this.connection = connection.ref();
    setDefaultSize(600, 1000);
    renameAction(getOKAction(), "Create Embeddings");
    renameAction(getCancelAction(), "Close");
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
  protected Action[] createActions() {
    return createActions(
            getOKAction(),
            getResetAction(),
            getCancelAction());
  }

  @NotNull
  private Action getResetAction() {
    return createAction("Reset", () -> getForm().reset());
  }

  @Override
  protected void doOKAction() {
    VectorAIForm form = getForm();
    form.applyFormChanges();
    VectorEmbeddingRequest request = form.getEmbeddingRequest();
    super.doOKAction();
    Runnable callbackInfo = () -> {
      request.resetSoft(); // softly reset the request after successful execution
      form.resetFormChanges();
      Messages.showInfoDialog(getProject(), "Embedding Succeeded ","Your data has been embedded successfully!");
    };
    Consumer<Exception> callbackError = (ex) -> Messages.showErrorDialog(getProject(), "Embedding Failed", ex.getMessage(), ex);
    DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(getProject());
    vectorManager.createEmbeddings(request, getConnection(), callbackInfo, callbackError);

  }

  @Override
  public void doCancelAction() {
    // capture the input even if not applied
    VectorAIForm form = getForm();
    form.applyFormChanges();

    super.doCancelAction();
  }
}