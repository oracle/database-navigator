package com.dbn.oci;

import com.oracle.oci.intellij.api.ext.UIModelContext;
import com.oracle.oci.intellij.api.oci.OCIDatabase;
import com.oracle.oci.intellij.api.oci.commands.DownloadWalletCommand;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.util.Map;
@Getter
@Setter
public class ConnectionSettings {
  private String displayName;
  private Boolean isMtlsConnectionRequired;
  private Map<String, String> allConnectionStrings;
  private boolean isDedicated;
  private  String configFile;
  private  String configProfile;
  OCIDatabase database;
  static ConnectionSettings toConnectionSettings(UIModelContext modelContext) {
    OCIDatabase database = (OCIDatabase) modelContext.getContextObject();
    ConnectionSettings connectionSettings = new ConnectionSettings();
    connectionSettings.setDatabase(database);
    connectionSettings.setDisplayName(database.getDisplayName());
    connectionSettings.setIsMtlsConnectionRequired(database.getIsMtlsConnectionRequired());
    connectionSettings.setAllConnectionStrings(database.getAllConnectionStrings());
    connectionSettings.setDedicated(database.isDedicated());
    connectionSettings.setConfigFile(modelContext.getConfigFile());
    connectionSettings.setConfigProfile(modelContext.getConfigProfile());
    return connectionSettings;
  }
  public String isWalletPresent(){
    return this.database.isWalletPresent();
  }

  public String getId() {
    return this.database.getId();
  }
  public String getCompartmentId() {
    return this.database.getCompartmentId();
  }
  public void downloadWallet(File walletLocation,String walletType,String password) throws Exception {
    DownloadWalletCommand command = new DownloadWalletCommand(database, walletLocation, walletType, password);
    command.execute();  }
}
