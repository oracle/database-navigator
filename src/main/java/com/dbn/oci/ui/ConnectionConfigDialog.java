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
import com.dbn.oci.ConnectionData;
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
import java.util.Random;

import static com.dbn.browser.DatabaseBrowserUtils.promoteNewConnection;

public class ConnectionConfigDialog extends DBNDialog<ConnectionConfigForm> implements NotificationSupport {
  private final ConnectionData connectionData;

  public ConnectionConfigDialog(Project project, String title, boolean canBeParent, ConnectionData connectionData) {
    super(project, title, canBeParent);
    this.connectionData = connectionData;
    init();
    setDefaultSize(468,363);
  }

  @Override
  protected @NotNull ConnectionConfigForm createForm() {
    return new ConnectionConfigForm(this, connectionData.isMtlsConnectionRequired(), connectionData.getParentCompartment());
  }

  @Override
  protected Action @NotNull [] createActions() {
    String okActionText = "Create Connection";
    getOKAction().putValue(Action.NAME, okActionText);
    getCancelAction().putValue(Action.NAME, "Close");
    return super.createActions();
  }

  @Override
  protected void doOKAction() {
    ConnectionConfigForm form = getForm();

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

  public String generateRandomPassword() {
    // Define character pools
    String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String numbers = "0123456789";
    String specialCharacters = "!@#$%^&*()-_=+[]{}|;:,.<>?/";

    Random random = new Random();
    StringBuilder password = new StringBuilder();

    // Add at least one letter
    password.append(letters.charAt(random.nextInt(letters.length())));

    // Add at least one number
    password.append(numbers.charAt(random.nextInt(numbers.length())));

    // Add at least one special character
    password.append(specialCharacters.charAt(random.nextInt(specialCharacters.length())));

    // Combine all character pools
    String allCharacters = letters + numbers + specialCharacters;

    // Fill the remaining characters up to 8
    while (password.length() < 8) {
      password.append(allCharacters.charAt(random.nextInt(allCharacters.length())));
    }

    // Shuffle the password
    char[] passwordArray = password.toString().toCharArray();
    for (int i = passwordArray.length - 1; i > 0; i--) {
      int index = random.nextInt(i + 1);
      char temp = passwordArray[i];
      passwordArray[i] = passwordArray[index];
      passwordArray[index] = temp;
    }

    // Return the shuffled password as a string
    return new String(passwordArray);
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

    Progress.prompt(getProject(), null, false, "Downloading wallet", "Downloading wallet for database \"" + connectionData.getDisplayName() + "\"",
            progress -> {
              if (preDownloadRunnable != null) {
                preDownloadRunnable.run();
              }
              downloadWallet(new File(walletLocation), "", password);
              showConnectionSettingsRunnable.run();
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
}
