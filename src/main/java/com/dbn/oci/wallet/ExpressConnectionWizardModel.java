package com.dbn.oci.wallet;

import com.dbn.oci.ConnectionSettings;
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
  ConnectionSettings connectionSettings;
  public ExpressConnectionWizardModel(String title, Project project, ConnectionSettings connectionSettings) {
    super(title);
    this.connectionSettings = connectionSettings;
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

  public ConnectionSettings getConnectionSettings() {
    return connectionSettings;
  }

  public List<WizardStep<ExpressConnectionWizardModel>> getMySteps() {
    return mySteps;
  }
}
