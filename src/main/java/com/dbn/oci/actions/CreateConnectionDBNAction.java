package com.dbn.oci.actions;

import com.dbn.connection.AuthenticationType;
import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.ConnectionConfigType;
import com.dbn.connection.config.tns.TnsImportData;
import com.dbn.connection.config.tns.TnsImportType;
import com.dbn.connection.config.tns.TnsNames;
import com.dbn.connection.config.tns.TnsNamesParser;
import com.dbn.oci.wallet.ExpressConnectionWizardDialog;
import com.dbn.options.ProjectSettingsManager;
import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.oracle.oci.intellij.api.ext.ContributeADBActions;
import com.oracle.oci.intellij.api.ext.UIModelContext;
import com.oracle.oci.intellij.api.oci.OCIDatabase;

import java.awt.event.ActionEvent;
import java.io.File;

public class CreateConnectionDBNAction extends ContributeADBActions.ExtensionContextAction {
  public static String WALLET_DEFAULT_LOCATION = System.getProperty("user.home") + "/.oci_toolkit/wallets/";
  UIModelContext uiModelContext;
  public CreateConnectionDBNAction(UIModelContext context, String title) {
    super(title);
    this.uiModelContext = context;
  }

  @Override
  protected void doAction(ActionEvent actionEvent) {
    DataContext dataContext = DataManager.getInstance().getDataContext();
    Project project =  dataContext.getData(CommonDataKeys.PROJECT);
    ProjectSettingsManager settingsManager = ProjectSettingsManager.getInstance(project);

    // verify if the db is mtls or not
    OCIDatabase database = (OCIDatabase) uiModelContext.getContextObject();
    if (!database.getIsMtlsConnectionRequired() ) {
      // fill the dialog with the connection string
      settingsManager.createConnection(DatabaseType.ORACLE, ConnectionConfigType.CUSTOM,uiModelContext);
    }else {
      // we need wallet
      //check if the wallet already exists
      String walletLocation = database.isWalletPresent();
      if (walletLocation== null){
        // wallet does not exist default location where to download the wallet
        walletLocation = WALLET_DEFAULT_LOCATION +database.getId();
       // download it
        boolean isOk = false;
        try {
          isOk = ExpressConnectionWizardDialog.showWizard(project,database);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
        if (!isOk){
          return;
        }
      }

      File tnsNamesFile = new File(walletLocation +"/tnsnames.ora");
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

      settingsManager.createConnections(tnsImportData,uiModelContext , AuthenticationType.TOKEN);
    }
  }
}
