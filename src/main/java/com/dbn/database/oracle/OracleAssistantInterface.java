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
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.DatabaseInterfaceBase;
import com.dbn.database.common.assistant.AssistantQueryResponse;
import com.dbn.database.common.util.BooleanResultSetConsumer;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;

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

  public AssistantQueryResponse generate(DBNConnection connection, String action, String profile, String attributes, String prompt) throws SQLException {
    return executeCall(connection, new AssistantQueryResponse(), "ai-generate", profile, action, attributes, prompt);
  }

  @Override
  public void grantPrivilege(DBNConnection connection, String username) throws SQLException {
    executeCall(connection, null, "grant-privilege", username);
  }

  @Override
  public void grantACLRights(DBNConnection connection, String command) throws SQLException {
    executeCall(connection, null, "acl-rights", command);
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
  public void createProfile(DBNConnection connection, String name, String attributes, String description) throws SQLException {
    executeUpdate(connection, "create-ai-profile", name, attributes, "ENABLED", description);
  }

  @Override
  public void updateProfile(DBNConnection connection, String name, String attributes) throws SQLException {
    executeUpdate(connection, "update-ai-profile", name, attributes);
  }
  @Override
  public void deleteProfile(DBNConnection connection, String ownerName, String profileName) throws SQLException {
    executeUpdate(connection, "drop-profile", /*ownerName, */profileName);
  }

  // TODO support foreign profile actions (ownerName)

  @Override
  public void enableProfile(DBNConnection connection, String ownerName, String profileName) throws SQLException {
    executeUpdate(connection, "enable-profile", /*ownerName, */profileName);
  }

  @Override
  public void disableProfile(DBNConnection connection, String ownerName, String profileName) throws SQLException {
    executeUpdate(connection, "disable-profile", /*ownerName, */profileName);
  }

  @Override
  public void setCurrentProfile(DBNConnection connection, String profileName) throws SQLException {
    executeUpdate(connection, "set-current-profile", profileName);
  }
}

