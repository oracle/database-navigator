package com.dbn.oci;

import com.dbn.connection.ConnectionId;
import com.oracle.oci.intellij.api.ext.UIModelContext;
import com.oracle.oci.intellij.api.oci.OCIDatabase;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;
@Getter
@Setter
public class ConnectionData {
  private String displayName;
  private boolean mtlsConnectionRequired;
  private Map<String, String> allConnectionStrings;
  private boolean dedicated;
  private String configFile;
  private String configProfile;
  private String parentCompartment;
  private String connectionName;
  private ConnectionId connectionId;
  private final OCIDatabase database;

  public ConnectionData(OCIDatabase database) {
    this.database = database;
  }

  static ConnectionData toConnectionSettings(UIModelContext modelContext) {
    OCIDatabase database = (OCIDatabase) modelContext.getContextObject();
    ConnectionData connectionData = new ConnectionData(database);
    String compId = modelContext.getContextObject().getCompartmentId();
    String dbIdentifier = database.getDisplayName()+"_"+database.getId().substring(database.getId().length()-8);
    String walletDefaultPath = database.getCompartmentName() + compId.substring(compId.length()-8)+"/"+dbIdentifier;
    connectionData.setParentCompartment(walletDefaultPath);

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
}
