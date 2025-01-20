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
  private String parentCompartment;
  OCIDatabase database;
  static ConnectionSettings toConnectionSettings(UIModelContext modelContext) {
    OCIDatabase database = (OCIDatabase) modelContext.getContextObject();
    ConnectionSettings connectionSettings = new ConnectionSettings();
    String compId = modelContext.getContextObject().getCompartmentId();
    String dbIdentifier = database.getDisplayName()+"_"+database.getId().substring(database.getId().length()-8);
    String walletDefaultPath = "comp"+ compId.substring(compId.length()-8)+"/"+dbIdentifier;
    connectionSettings.setParentCompartment(walletDefaultPath);

    connectionSettings.setDatabase(database);
    connectionSettings.setDisplayName(database.getDisplayName());
    connectionSettings.setIsMtlsConnectionRequired(database.getIsMtlsConnectionRequired());
    connectionSettings.setAllConnectionStrings(database.getTlsConnectionStrings().getAllConnectionStrings());
    connectionSettings.setConfigFile(modelContext.getConfigFile());
    connectionSettings.setConfigProfile(modelContext.getConfigProfile());
    return connectionSettings;
  }

  public String getId() {
    return this.database.getId();
  }
  public String getCompartmentId() {
    return this.database.getCompartmentId();
  }
  public boolean downloadWallet(File walletLocation,String walletType,String password)  {


        DownloadWalletCommand command = new DownloadWalletCommand(database, walletLocation, walletType, password);
        try {
          command.execute();
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
    return true;
  }
}
