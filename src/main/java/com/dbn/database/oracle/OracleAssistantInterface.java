/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.database.oracle;

import com.dbn.assistant.DatabaseAssistantType;
import com.dbn.assistant.entity.Credential;
import com.dbn.assistant.entity.DBObjectItem;
import com.dbn.assistant.entity.DatabaseObjectType;
import com.dbn.assistant.entity.Profile;
import com.dbn.assistant.service.exception.CredentialManagementException;
import com.dbn.assistant.service.exception.DatabaseOperationException;
import com.dbn.assistant.service.exception.ProfileManagementException;
import com.dbn.assistant.service.exception.QueryExecutionException;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.DatabaseInterfaceBase;
import com.dbn.database.common.assistant.*;
import com.dbn.database.common.util.BooleanResultSetConsumer;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Oracle specialized database interface responsible for interactions related to AI-Assistance
 *
 * @author Ayoub Aarrasse (Oracle)
 * @author Emmanuel Jannetti (Oracle)
 */
@Slf4j
public class OracleAssistantInterface extends DatabaseInterfaceBase implements DatabaseAssistantInterface {

  public OracleAssistantInterface(DatabaseInterfaces provider) {
    super("oracle_ai_interface.xml", provider);
  }

  @Override
  public void createCredential(DBNConnection connection, Credential credentialAttributes) throws CredentialManagementException {
    try {
      executeCall(connection, null, "create-credential", credentialAttributes.getName(), credentialAttributes.toAttributeMap());
    } catch (SQLException e) {
      throw new CredentialManagementException("Failed to create credential: " + credentialAttributes.getName(), e);
    }
  }

  @Override
  public void dropCredential(DBNConnection connection, String credentialName) throws CredentialManagementException {
    try {
      executeCall(connection, null, "drop-credential", credentialName);
    } catch (SQLException e) {
      throw new CredentialManagementException("Failed to drop credential: " + credentialName, e);
    }
  }

  @Override
  public void setCredentialAttribute(DBNConnection connection, Credential credential) throws CredentialManagementException {

    List<String> updatedList = credential.toUpdatingAttributeList();

    //TODO make these requests run simultaneously
    for (String updatedAttr : updatedList) {
      try {
        executeCall(connection, null, "update-credential", updatedAttr);
      } catch (SQLException e) {
        throw new CredentialManagementException("Failed to set credential attribute: " + credential.getName(), e);
      }
    }

  }

  @Override
  public void createProfile(DBNConnection connection, Profile profileAttributes) throws ProfileManagementException {
    try {
      executeCall(connection, null, "create-profile", profileAttributes.getProfileName(), profileAttributes.toAttributeMap());
    } catch (SQLException e) {
      throw new ProfileManagementException("Failed to create profile: " + profileAttributes.getProfileName() + "\n" + e.getMessage(), e);
    }
  }

  @Override
  public void createProfile(DBNConnection connection, String name, String attributes, String description) throws SQLException {
    executeUpdate(connection, "create-ai-profile", name, attributes, "ENABLED", description);
  }

  @Override
  public void updateProfile(DBNConnection connection, String name, String attributes) throws SQLException {
    executeUpdate(connection, "update-ai-profile", name, attributes);
  }

  @Override
  public void dropProfile(DBNConnection connection, String profileName) throws ProfileManagementException {
    try {
      executeCall(connection, null, "drop-profile", profileName);
    } catch (SQLException e) {
      throw new ProfileManagementException("Failed to drop profile: " + profileName, e);
    }
  }

  @Override
  public void setProfileAttributes(DBNConnection connection, Profile profile) throws ProfileManagementException {
    if (log.isDebugEnabled()) {
      log.debug("setProfileAttributes for " + profile);
      log.debug("   attribute map " + profile.toAttributeMap());
    }
    try {
      executeCall(connection, null, "update-profile", profile.toAttributeMap());
    } catch (SQLException e) {
      throw new ProfileManagementException("Failed to set profile attribute: " + profile.getProfileName(), e);
    }
  }

  @Override
  public OracleQueryOutput executeQuery(DBNConnection connection, String action, String profile, String text, String model) throws QueryExecutionException {
    try {
      return executeCall(connection, new OracleQueryOutput(), "ai-query", profile, action, text.replace("'", "''"), "{\"model\":\"" + model + "\"}");
    } catch (SQLException e) {
      throw new QueryExecutionException("Failed to query\n", e);
    }
  }

  public AssistantQueryResponse generate(DBNConnection connection, String action, String profile, String model, String prompt) throws SQLException {
    prompt = prompt.replaceAll("'", "''");
    String attributes = "{\"model\":\"" + model + "\"}";
    return executeCall(connection, new AssistantQueryResponse(), "ai-generate", profile, action, prompt, attributes);
  }


  @Override
  public OracleTablesList listTables(DBNConnection connection) throws DatabaseOperationException {
    try {
      return executeCall(connection, new OracleTablesList(), "list-tables");
    } catch (SQLException e) {
      throw new DatabaseOperationException("Failed to list tables", e);
    }
  }

  @Override
  public OracleViewsList listViews(DBNConnection connection) throws DatabaseOperationException {
    try {
      return executeCall(connection, new OracleViewsList(), "list-views");
    } catch (SQLException e) {
      throw new DatabaseOperationException("Failed to list views", e);
    }
  }


  @Override
  public List<Credential> listCredentials(DBNConnection connection) throws CredentialManagementException {
    try {
      List<Credential> credentials = executeCall(connection, new OracleCredentialsDetailedInfo(), "list-credentials-detailed").getCredentialsProviders();
      return credentials;
    } catch (SQLException e) {
      throw new CredentialManagementException("Failed to list credentials", e);
    }
  }

  @Override
  public List<Profile> listProfiles(DBNConnection connection) throws ProfileManagementException {
    try {
      List<Profile> profileList = executeCall(connection, new OracleProfilesAttributesInfo(), "list-profiles-detailed").getProfileList();
      return profileList;
    } catch (SQLException e) {
      throw new ProfileManagementException(e.getMessage(), e);
    }
  }

  @Override
  public List<String> listSchemas(DBNConnection connection) throws DatabaseOperationException {
    try {
      List<String> schemaList = executeCall(connection, new SchemasInfo(), "list-schemas").getSchemaList();
      return schemaList;
    } catch (SQLException e) {
      throw new DatabaseOperationException(e.getMessage(), e);
    }
  }

  @Override
  public List<DBObjectItem> listObjectListItems(DBNConnection connection, String schemaName) throws DatabaseOperationException {
    try {
      List<DBObjectItem> DBObjectItems = new ArrayList<>();
      // TODO : should be one roundtrip
      //    refactor later
      List<DBObjectItem> tableDBObjectListItems = executeCall(connection, new TableAndViewListInfo(schemaName, DatabaseObjectType.TABLE), "list-tables", schemaName).getDBObjectListItems();
      List<DBObjectItem> viewDBObjectListItems = executeCall(connection, new TableAndViewListInfo(schemaName, DatabaseObjectType.VIEW), "list-views", schemaName).getDBObjectListItems();
      List<DBObjectItem> materializedViewDBObjectListItems = executeCall(connection, new TableAndViewListInfo(schemaName, DatabaseObjectType.MATERIALIZED_VIEW), "list-materialized-views", schemaName).getDBObjectListItems();
      DBObjectItems.addAll(tableDBObjectListItems);
      DBObjectItems.addAll(viewDBObjectListItems);
      DBObjectItems.addAll(materializedViewDBObjectListItems);
      return DBObjectItems;
    } catch (SQLException e) {
      throw new DatabaseOperationException(e.getMessage(), e);
    }
  }

  @Override
  public void updateCredentialStatus(DBNConnection connection, String credentialName, boolean isEnabled) throws CredentialManagementException {
    try {
      executeCall(connection, null, isEnabled ? "enable-credential" : "disable-credential", credentialName);
    } catch (SQLException e) {
      throw new CredentialManagementException("Failed to disable credential: " + credentialName, e);
    }
  }

  @Override
  public void checkAdmin(DBNConnection connection) throws SQLException {
    executeCall(connection, null, "check-admin");
  }

  @Override
  public void grantPrivilege(DBNConnection connection, String username) throws SQLException {
    try {
      executeCall(connection, null, "grant-privilege", username);
    } catch (SQLException e) {
      throw new SQLException("Failed to grant privilege\n" + e.getMessage());
    }
  }

  @Override
  public void grantACLRights(DBNConnection connection, String command) throws SQLException {
    try {
      executeCall(connection, null, "acl-rights", command);
    } catch (SQLException e) {
      throw new SQLException("Failed to grant privilege\n" + e.getMessage());
    }
  }

  @Override
  public boolean isAssistantFeatureSupported(DBNConnection connection) throws SQLException {
    return BooleanResultSetConsumer.INSTANCE.consume(() -> executeQuery(connection, "is-feature-supported"));
  }

  @Override
  public DatabaseAssistantType getAssistantType(DBNConnection connection) throws SQLException {
    return isAssistantFeatureSupported(connection) ?
            DatabaseAssistantType.SELECT_AI :
            DatabaseAssistantType.GENERIC;
  }


  @Override
  public void createPwdCredential(DBNConnection connection, String credentialName, String userName, String password) throws SQLException {
    executeUpdate(connection, "create-password-credential", credentialName, userName, password);
  }

  @Override
  public void createOciCredential(DBNConnection connection, String credentialName, String userOcid, String tenancyOcid, String privateKey, String fingerprint) throws SQLException {
    executeUpdate(connection, "create-oci-credential", credentialName, userOcid, tenancyOcid, privateKey, fingerprint);
  }

  @Override
  public void updateCredentialAttribute(DBNConnection connection, String credentialName, String attribute, String value) throws SQLException {
    executeUpdate(connection, "update-credential-attribute", credentialName, attribute, value);
  }

  @Override
  public void enableCredential(DBNConnection connection, String credentialName) throws SQLException {
    executeUpdate(connection, "enable-credential", credentialName);
  }

  @Override
  public void disableCredential(DBNConnection connection, String credentialName) throws SQLException {
    executeUpdate(connection, "disable-credential", credentialName);
  }

  @Override
  public void deleteCredential(DBNConnection connection, String credentialName) throws SQLException {
    executeUpdate(connection, "drop-credential", credentialName);
  }

  @Override
  public void setCurrentProfile(DBNConnection connection, String profileName) throws SQLException {
    executeUpdate(connection, "set-current-profile", profileName);
  }
}

