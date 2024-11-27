package com.dbn.oci.wallet;

import com.dbn.oci.wallet.steps.ConnectionDownloadWalletProgressStep;
import com.dbn.oci.wallet.steps.DownloadWalletStep;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.wizard.WizardModel;
import com.intellij.ui.wizard.WizardStep;
import com.oracle.oci.intellij.api.oci.OCIDatabase;

import java.util.List;

public class ExpressConnectionWizardModel extends WizardModel implements Disposable {
  List<WizardStep<ExpressConnectionWizardModel>> mySteps;
  OCIDatabase database;
  public ExpressConnectionWizardModel(String title, Project project,OCIDatabase database) {
    super(title);
    this.database = database;
    mySteps = List.of(
            new DownloadWalletStep(project),
            new ConnectionDownloadWalletProgressStep()
    );

    mySteps.forEach(this::add);
    mySteps.forEach(s-> Disposer.register(this, (Disposable) s));
  }

  @Override
  public void dispose() {
    // TODO dispose UI resources
  }

  public OCIDatabase getDatabase() {
    return database;
  }

  public List<WizardStep<ExpressConnectionWizardModel>> getMySteps() {
    return mySteps;
  }
}
