/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.database.interfaces;

import com.dbn.assistant.AssistantType;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.assistant.AssistantQueryResponse;
import com.dbn.object.factory.ModelFactoryInput;
import com.dbn.vector.model.chunk.ChunkConfiguration;
import com.dbn.vector.model.embed.EmbedConfig;
import com.dbn.vector.model.sourceconfig.DBTableSourceConfig;
import com.dbn.vector.model.store.StoreConfig;

import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
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
  public AssistantQueryResponse generateRag(DBNConnection connection, String prompt) throws SQLException ;

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

  void enableDataAccess(DBNConnection connection) throws SQLException;

  void disableDataAccess(DBNConnection connection) throws SQLException;

  default AssistantType getAssistantType(DBNConnection connection) throws SQLException {
    return AssistantType.PUBLIC;
  }

  void loadOnnxModelFromOci(ModelFactoryInput input, DBNConnection conn) throws SQLException;
  void deleteAIModel(DBNConnection conn,String modelName) throws SQLException;
  void loadOnnxModelThroughJdbc(String modelName, Blob modelBlob, DBNConnection conn) throws SQLException;
  ResultSet chunk(String text, ChunkConfiguration chunkConfiguration, DBNConnection conn) throws SQLException;

  void embed(DBNConnection connection, DBTableSourceConfig sourceConfig, ChunkConfiguration chunkConfiguration, EmbedConfig embedConfig, StoreConfig storeConfig) throws SQLException;
  void createTable(DBNConnection connection, String tableName) throws SQLException;

  void embed(DBNConnection conn, Clob sourceFileClob, ChunkConfiguration chunkConfiguration, EmbedConfig embedConfig, StoreConfig storeConfig) throws SQLException;
  void embed(DBNConnection conn, Blob sourceFileClob, ChunkConfiguration chunkConfiguration, EmbedConfig embedConfig, StoreConfig storeConfig) throws SQLException;
}
