package com.dbn.vector.ui;

import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.help.HelpTopic;
import com.dbn.vector.DatabaseVectorManager;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.service.VectorEmbeddingRequestVerifier;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.nls.NlsResources.txt;

public class VectorToolboxDialog extends DBNDialog<VectorToolboxForm> {
  private final VectorEmbeddingRequest request;

  public VectorToolboxDialog(ConnectionHandler connection, VectorEmbeddingRequest request) {
    super(connection, txt("msg.vector.title.VectorToolbox"), true);
    this.request = request;

    setDefaultSize(680, 1000);

    if (!request.isTemplate()) {
      VectorToolboxForm toolboxForm = getForm();
      toolboxForm.freezeForm();
    }

    init();
  }

  @Override
  protected HelpTopic getHelpTopic() {
    return HelpTopic.VECTOR_TOOLBOX;
  }

  @Override
  protected @NotNull VectorToolboxForm createForm() {
    return new VectorToolboxForm(this, request);
  }

  @Override
  protected Action[] initializeActions() {
    renameAction(getOKAction(), txt("msg.vector.button.CreateEmbeddings"));
    renameAction(getCancelAction(), txt("msg.shared.button.Close"));

    return request.isTemplate() ?
            actions(
                getOKAction(),
                getResetAction(),
                getCancelAction()) :
            actions(getCancelAction());
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

    verifyAndSubmit();
  }

  private void verifyAndSubmit() {
    Progress.modal(ensureProject(), request.getConnection(), true,
            txt("prc.vector.title.VerifyingRequest"),
            txt("prc.vector.text.VerifyingEmbeddingRequest"), i -> {
        if (!VectorEmbeddingRequestVerifier.verifyRequest(request, i)) return;

        dispatch(() -> {
          super.doOKAction();
          DatabaseVectorManager vectorManager = DatabaseVectorManager.getInstance(getProject());
          vectorManager.createEmbeddings(request);
        });
    });
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
