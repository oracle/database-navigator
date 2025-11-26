package com.dbn.vector.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.VectorEmbeddingRequest;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class VectorToolboxDialog extends DBNDialog<VectorToolboxForm> {
  private final ConnectionRef connection;
  private final VectorEmbeddingRequest request;

  public VectorToolboxDialog(ConnectionHandler connection, VectorEmbeddingRequest request) {
    super(connection.getProject(), "Vector Toolbox", true);
    this.connection = connection.ref();
    this.request = request;

    setDefaultSize(600, 1000);
    renameAction(getOKAction(), "Create Embeddings");
    renameAction(getCancelAction(), "Close");

    if (!request.isTemplate()) {
      VectorToolboxForm toolboxForm = getForm();
      toolboxForm.freezeForm();
    }

    init();
  }

  private ConnectionHandler getConnection() {
    return connection.ensure();
  }

  @Override
  protected @NotNull VectorToolboxForm createForm() {
    return new VectorToolboxForm(this, getConnection(), request);
  }

  @Override
  protected Action[] createActions() {

    return request.isTemplate() ?
            createActions(
              getOKAction(),
              getResetAction(),
              getCancelAction()) :
            createActions(
                    getCancelAction());
  }

  @NotNull
  private Action getResetAction() {
    return createAction("Reset", () -> getForm().reset());
  }

  @Override
  protected void doOKAction() {
    VectorToolboxForm form = getForm();
    form.applyFormChanges();
    if (request.isTemplate()) {
      form.saveRequestTemplate(true);
    }


    super.doOKAction();
    DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(getProject());
    vectorManager.createEmbeddings(request, getConnection());
  }

  @Override
  public void doCancelAction() {
    // capture the input even if not applied
    VectorToolboxForm form = getForm();
    form.applyFormChanges();

    if (request.isTemplate()) {
      form.saveRequestTemplate(false);
    }

    super.doCancelAction();
  }
}