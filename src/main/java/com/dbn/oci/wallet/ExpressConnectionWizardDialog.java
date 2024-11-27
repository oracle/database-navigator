package com.dbn.oci.wallet;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.wizard.WizardDialog;
import com.oracle.oci.intellij.api.oci.OCIDatabase;

import java.util.concurrent.atomic.AtomicBoolean;


public class ExpressConnectionWizardDialog extends WizardDialog<ExpressConnectionWizardModel> {
  public ExpressConnectionWizardDialog(Project project, boolean canBeParent, ExpressConnectionWizardModel model) {
    super(project, canBeParent, model);

  }
  public static boolean showWizard(Project project, OCIDatabase db) {
    AtomicBoolean isOk = new AtomicBoolean(false);
    ApplicationManager.getApplication().invokeAndWait(()->{
      ExpressConnectionWizardDialog dialog = new ExpressConnectionWizardDialog(project, true, new ExpressConnectionWizardModel("Download Wallet",project,db));
       isOk.set(dialog.showAndGet());
    });
    return isOk.get();
  }

}
