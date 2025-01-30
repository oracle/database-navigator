package com.dbn.oci.ui;

import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionConfigType;
import com.dbn.connection.config.tns.TnsImportData;
import com.dbn.connection.config.tns.TnsImportType;
import com.dbn.connection.config.tns.TnsNames;
import com.dbn.connection.config.tns.TnsNamesParser;
import com.dbn.oci.ConnectionData;
import com.dbn.options.ProjectSettingsManager;
import com.intellij.ide.DataManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ValidationInfo;
import com.oracle.oci.intellij.ui.common.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

public class ConnectionConfigDialog extends DBNDialog<ConnectionConfigForm>  {
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
  protected @NotNull List<ValidationInfo> doValidateAll() {
    List<ValidationInfo> validationInfos = new ArrayList<>();

    if (!getForm().isMTLS() || !getForm().isSpecifyPassword())
      return new ArrayList<>();
    ValidationInfo passwordValidation =  getForm().validatePassword();
    ValidationInfo confirmPasswordValidation =  getForm().validateConfirmPassword();

    Optional.ofNullable(passwordValidation).ifPresent(validationInfos::add);
    Optional.ofNullable(confirmPasswordValidation).ifPresent(validationInfos::add);

    return validationInfos;
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
    //todo need to be improved

    if (form.isMTLS() && (form.isDirtyWalletPath() || !form.validateWalletPath())){
      return;
    }
    DataContext dataContext = DataManager.getInstance().getDataContext();
    Project project = dataContext.getData(CommonDataKeys.PROJECT);
    ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(project);

    if (form.isMTLS()) {
      connectionData.setConnectionName(connectionData.getDisplayName()+"_MTLS");
      String walletLocation = form.getWalletLocation();
      String password = form.getPassword();
      password = form.isSpecifyPassword()?password: generateRandomPassword();

      if (form.getWalletPathType().equals(ConnectionConfigForm.WalletPathType.EMPTY_FOLDER)){
        downloadNewWallet(walletLocation,password,null,()->openSettings(settingsManager,walletLocation));
      }else {
        openSettings(settingsManager,walletLocation);
      }

    } else {
      connectionData.setConnectionName(connectionData.getDisplayName()+"_TLS");
      settingsManager.createConnection(DatabaseType.ORACLE, ConnectionConfigType.CUSTOM, connectionData);
    }
    close(OK_EXIT_CODE);


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

  private void openSettings(ProjectSettingsManager settingsManager,String walletLocation){
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

    settingsManager.createConnections(tnsImportData, connectionData);
  }

  private void downloadNewWallet(String walletLocation, String password,Runnable preDownloadRunnable ,Runnable showConnectionSettingsRunnable) {
    if (walletLocation == null || walletLocation.isEmpty()) {
      UIUtil.fireNotification(NotificationType.ERROR, "Invalid wallet location.");
      return;
    }

    Progress.prompt(getProject(),null,false,"Downloading wallet","Downloading wallet",
            progress -> {
              if (preDownloadRunnable != null) {
                preDownloadRunnable.run();
              }
              boolean downloadSuccess = connectionData.downloadWallet(
                      new File(walletLocation ),
                      "",
                      password
              );

              if (downloadSuccess) {
                UIUtil.fireNotification(NotificationType.INFORMATION, "The wallet has been downloaded successfully.");
              } else {
                UIUtil.fireNotification(NotificationType.ERROR, "Failed to download the wallet.");
              }
              try {
                showConnectionSettingsRunnable.run();
              }catch (Exception ex){
                throw new RuntimeException(ex);
              }

            }
            );

  }


}
