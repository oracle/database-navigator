package com.dbn.oci.ui;

import com.dbn.common.notification.NotificationGroup;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Dialogs.DialogCallback;
import com.dbn.common.util.Messages;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionConfigType;
import com.dbn.connection.config.tns.TnsImportData;
import com.dbn.connection.config.tns.TnsImportType;
import com.dbn.connection.config.tns.TnsNames;
import com.dbn.connection.config.tns.TnsNamesParser;
import com.dbn.oci.OciConnectionData;
import com.dbn.options.ProjectSettingsManager;
import com.dbn.options.ui.ProjectSettingsDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.oracle.oci.intellij.api.oci.OCIDatabase;
import com.oracle.oci.intellij.api.oci.commands.DownloadWalletCommand;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;
import java.io.File;
import java.io.IOException;

import static com.dbn.browser.DatabaseBrowserUtils.promoteNewConnection;
import static com.dbn.oci.util.WalletPasswordGenerator.generateRandomPassword;

public class OciConnectionInputDialog extends DBNDialog<OciConnectionInputForm> implements NotificationSupport {
  private final OciConnectionData connectionData;

  public OciConnectionInputDialog(Project project, String title, boolean canBeParent, OciConnectionData connectionData) {
    super(project, title, canBeParent);
    this.connectionData = connectionData;
    setDefaultSize(640,320);

    init();
  }

  @Override
  protected @NotNull OciConnectionInputForm createForm() {
    return new OciConnectionInputForm(this, connectionData.isMtlsConnectionRequired(), connectionData.getParentCompartment());
  }

  @Override
  protected Action @NotNull [] createActions() {
    renameAction(getOKAction(), "Create Connection");
    return super.createActions();
  }

  @Override
  protected void doOKAction() {
    OciConnectionInputForm form = getForm();

    Project project = ensureProject();
    ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(project);

    // collect form data before dialog is closed
    boolean mutualTls = form.isMTLS();
    boolean walletDownloadRequired = form.isWalletDownload();
    String walletLocation = form.getWalletLocation();
    String password = form.isSpecifyPassword() ?
            form.getPassword() :
            generateRandomPassword();

    close(OK_EXIT_CODE);
    String databaseName = connectionData.getDisplayName();
    if (mutualTls) {
      connectionData.setConnectionName(databaseName + "_MTLS");

      if (walletDownloadRequired) {
        downloadNewWallet(walletLocation, password, null, () -> openSettings(settingsManager, walletLocation, promoteConnectionCallback()));
      } else {
        openSettings(settingsManager, walletLocation, promoteConnectionCallback());
      }

    } else {
      connectionData.setConnectionName(databaseName + "_TLS");
      settingsManager.createConnection(DatabaseType.ORACLE, ConnectionConfigType.CUSTOM, connectionData, promoteConnectionCallback());
    }
  }

  private DialogCallback<ProjectSettingsDialog> promoteConnectionCallback() {
    return (dialog, exitCode) -> {
      if (exitCode == OK_EXIT_CODE) {
        Project project = getProject();
        promoteNewConnection(project, connectionData.getConnectionId());
      }
    };
  }


  private void openSettings(ProjectSettingsManager settingsManager, String walletLocation, DialogCallback<ProjectSettingsDialog> callback){
    File tnsNamesFile = new File(walletLocation + "/tnsnames.ora");
    TnsNames tnsNames;
    try {
      tnsNames = TnsNamesParser.get(tnsNamesFile);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    // populate with the first profile .
    tnsNames.getProfiles().get(0).setSelected(true);
    TnsImportData tnsImportData = new TnsImportData();
    tnsImportData.setImportType(TnsImportType.PROFILE);
    tnsImportData.setTnsNames(tnsNames);
    tnsImportData.setSelectedOnly(true);

    settingsManager.createConnection(tnsImportData, connectionData, callback);
  }

  private void downloadNewWallet(String walletLocation, String password, Runnable preDownloadRunnable, Runnable showConnectionSettingsRunnable) {
    if (walletLocation == null || walletLocation.isEmpty()) {
      Messages.showErrorDialog(getProject(), "Missing Wallet Location", "Wallet location not specified.");
      return;
    }

    Progress.prompt(getProject(), null, true, "Downloading wallet", "Downloading wallet for database \"" + connectionData.getDisplayName() + "\"",
            progress -> {
              if (preDownloadRunnable != null) {
                preDownloadRunnable.run();
              }
              File walletLocationFile = new File(walletLocation);
              downloadWallet(walletLocationFile, "", password);
              if (progress.isCanceled()){
                cleanupDownloadFolder(walletLocationFile);
              }else {
                showConnectionSettingsRunnable.run();
              }
            }
    );
  }

  private void downloadWallet(File walletLocation, String walletType, String password) {
    OCIDatabase database = connectionData.getDatabase();
    String displayName = database.getDisplayName();

    try {
      prepareWalletLocation(walletLocation);

      DownloadWalletCommand command = new DownloadWalletCommand(database, walletLocation, walletType, password);
      command.execute();
      // soft non-intrusive notification
      sendInfoNotification(NotificationGroup.CONNECTION, "The wallet for database \"" + displayName + "\" was downloaded successfully.");
    } catch (Exception e) {
      // error prompt
      Messages.showErrorDialog(getProject(), "Wallet Download Failed", "Failed to download the wallet for database " + displayName + ".", e);
    }
  }

  private static void prepareWalletLocation(File location) throws IOException {
    String path = location.getAbsolutePath();

    // make sure the full path is available
    // (oci download command only attempts to create last directory in the path)
    if (!FileUtil.createDirectory(location)) {
      throw new IOException("Could not create wallet directory \"" + path + "\"");
    }

    // verify if available
    if (!location.exists() || !location.isDirectory()) {
      throw new IOException("Could not create wallet directory \"" + path + "\"");
    }

    // verify if location is empty (prevent blind overwrite)
    File[] files = location.listFiles();
    if (files != null && files.length > 0) {
      throw new IOException("Wallet directory \"" + path + "\" is not empty");
    }
  }

  private void cleanupDownloadFolder(File directory) {
    if (directory.exists()) {
      File[] files = directory.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.isFile()) {
            file.delete();
          }
        }
      }
    }
  }
}
