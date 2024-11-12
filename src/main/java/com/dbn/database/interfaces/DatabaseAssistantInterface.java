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

package com.dbn.database.interfaces;

import com.dbn.assistant.DatabaseAssistantType;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.assistant.AssistantQueryResponse;

import java.sql.SQLException;

/**
 * Defines the interface for managing Oracle AI profiles and credentials in a database.
 * This includes creating, updating, and deleting credentials and profiles,
 * executing AI-related queries, and listing database tables, views, credentials, and profiles.
 *
 * @author Ayoub Aarrasse (Oracle)
 */
public interface DatabaseAssistantInterface extends DatabaseInterface {

  /**
   * Executes an AI-related query using a specified action and text on a specific profile.
   *
   * @param connection The database connection object.
   * @param action     The AI action to perform, such as translate or analyze.
   * @param profile    The name of the AI profile to use for the query execution.
   * @param attributes The attributes qualifying the request
   * @param prompt     The text or query to process using the AI action.
   * @return The result of the AI query execution.
   * @throws SQLException If there is an error in executing the AI query.
   */
  AssistantQueryResponse generate(DBNConnection connection, String action, String profile, String attributes, String prompt) throws SQLException;

  /**
   * Grant a user the necessary privileges to access needed packages (DBMS_CLOUD, DBMS_CLOUD_AI)
   *
   * @param connection The database connection object.
   * @param username   The username to be granted privileges.
   */
  void grantPrivilege(DBNConnection connection, String username) throws SQLException;

  /**
   * Gives ACL rights to communicate with AI provider
   *
   * @param connection The database connection object.
   * @param command    The full PL/SQL command.
   */
  void grantACLRights(DBNConnection connection, String command) throws SQLException;

  /**
   * Verifies if the database AI-Assistant backend is available
   *
   * @param connection The database connection to use for interaction
   * @return true if the assistant feature is supported, false otherwise
   * @throws SQLException if the interaction with the database was unsuccessful
   */
  boolean isAssistantFeatureSupported(DBNConnection connection) throws SQLException;

  void createPwdCredential(DBNConnection connection, String credentialName, String userName, String password) throws SQLException;

  void createOciCredential(DBNConnection connection, String credentialName, String userOcid, String tenancyOcid, String privateKey, String fingerprint) throws SQLException;

  void updateCredentialAttribute(DBNConnection connection, String credentialName, String attribute, String value) throws SQLException;

  void enableCredential(DBNConnection connection, String credentialName) throws SQLException;

  void disableCredential(DBNConnection connection, String credentialName) throws SQLException;

  void deleteCredential(DBNConnection connection, String credentialName) throws SQLException;

  void setCurrentProfile(DBNConnection connection, String profileName) throws SQLException;

  void createProfile(DBNConnection connection, String name, String attributes, String description) throws SQLException;

  void updateProfile(DBNConnection connection, String name, String attributes) throws SQLException;

  void deleteProfile(DBNConnection connection, String ownerName, String profileName) throws SQLException;

  void enableProfile(DBNConnection connection, String ownerName, String profileName) throws SQLException;

  void disableProfile(DBNConnection connection, String ownerName, String profileName) throws SQLException;


  default DatabaseAssistantType getAssistantType(DBNConnection connection) throws SQLException {
    return DatabaseAssistantType.GENERIC;
  }
}
