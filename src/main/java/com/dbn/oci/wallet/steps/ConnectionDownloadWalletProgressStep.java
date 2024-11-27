package com.dbn.oci.wallet.steps;

import com.dbn.oci.wallet.ExpressConnectionWizardModel;
import com.intellij.openapi.Disposable;
import com.intellij.ui.wizard.WizardNavigationState;
import com.intellij.ui.wizard.WizardStep;
import com.oracle.oci.intellij.api.oci.OCIDatabase;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;

public class ConnectionDownloadWalletProgressStep extends WizardStep<ExpressConnectionWizardModel> implements Disposable {
  JPanel mainPanel;
  private JProgressBar downloadProgress;
  private JLabel statusLabel;

  @Override
  public JComponent prepare(WizardNavigationState wizardNavigationState) {
    return mainPanel;
  }

  @Override
  public void dispose() {

  }

  public void startDownload(OCIDatabase database, String password, @NotNull String walletLocation) {
    new Thread(() -> {
      try {
        database.downloadWallet(database,new File(walletLocation+database.getId()),"",password);

        SwingUtilities.invokeLater(() -> {
          downloadProgress.setIndeterminate(false);
          downloadProgress.setValue(100);
          downloadProgress.setStringPainted(true);
          statusLabel.setText("Download complete.");
        });
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }).start();
  }

}
