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
public class ConnectionData {
  private String displayName;
  private boolean mtlsConnectionRequired;
  private Map<String, String> allConnectionStrings;
  private boolean dedicated;
  private  String configFile;
  private  String configProfile;
  private String parentCompartment;
  private String connectionName;
  OCIDatabase database;
  static ConnectionData toConnectionSettings(UIModelContext modelContext) {
    OCIDatabase database = (OCIDatabase) modelContext.getContextObject();
    ConnectionData connectionData = new ConnectionData();
    String compId = modelContext.getContextObject().getCompartmentId();
    String dbIdentifier = database.getDisplayName()+"_"+database.getId().substring(database.getId().length()-8);
    String walletDefaultPath = "comp"+ compId.substring(compId.length()-8)+"/"+dbIdentifier;
    connectionData.setParentCompartment(walletDefaultPath);

    connectionData.setDatabase(database);
    connectionData.setDisplayName(database.getDisplayName());
    connectionData.setMtlsConnectionRequired(database.getIsMtlsConnectionRequired());
    connectionData.setAllConnectionStrings(database.getTlsConnectionStrings().getAllConnectionStrings());
    connectionData.setConfigFile(modelContext.getConfigFile());
    connectionData.setConfigProfile(modelContext.getConfigProfile());
    return connectionData;
  }

  public String getOcid() {
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
