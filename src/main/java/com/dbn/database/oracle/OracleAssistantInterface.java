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

package com.dbn.database.oracle;

import com.dbn.assistant.AssistantType;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.DatabaseInterfaceBase;
import com.dbn.database.common.statement.output.ClobOutput;
import com.dbn.database.common.util.BooleanResultSetConsumer;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.object.factory.ModelFactoryInput;
import com.dbn.vector.model.sourceconfig.DBTableSourceConfig;
import com.dbn.vector.model.store.StoreConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
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

  public String generate(DBNConnection connection, String action, String profile, String attributes, String prompt) throws SQLException {
    return executeCall(connection, new ClobOutput(), "ai-generate", profile, action, attributes, prompt).getValue();
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
  public AssistantType getAssistantType(DBNConnection connection) throws SQLException {
    return isAssistantFeatureSupported(connection) ?
            AssistantType.SELECT_AI :
            AssistantType.PUBLIC;
  }

  @Override
  public void loadOnnxModelFromOci(ModelFactoryInput input, DBNConnection conn) throws SQLException {
    executeUpdate(conn,"load-onnx-model-from-object-storage",input.getModelName(), input.getCredentialName(), input.getSourceLocation());
  }

  @Override
  public void deleteAIModel(DBNConnection conn,String modelName) throws SQLException {
    executeUpdate(conn,"drop-embed-model",modelName);
  }

  @Override
  public ResultSet chunkTextContent(String text, String chunkBy, String splitBy, int max, int overlap, DBNConnection conn) throws SQLException {
    return executeQuery(conn,"chunk-text-from-chunk-lab", text, chunkBy, max, overlap, splitBy);
  }

  @Override
  public void embed(DBNConnection conn, DBTableSourceConfig sourceConfig, String chunkConfig, String embedConfig, StoreConfig storeConfig) throws SQLException {
    executeUpdate(conn,
            "insert-vector-embeddings",
            storeConfig.getTableName(),         // {0} -> target table
            sourceConfig.getTableName(),        // {1} -> source table
            sourceConfig.getDataColumnName(),   // {2} -> source column
            chunkConfig,                        // {3} -> chunk config JSON
            embedConfig                         // {4} -> embed config JSON
    );
  }

  @Override
  public void embed(DBNConnection conn, String  blobId, String blob_table, String chunkConfig, String embedConfig, StoreConfig storeConfig) throws SQLException {
      executeUpdate(conn,
              "insert-vector-embeddings-from-filesystem",
              storeConfig.getTableName(),
              blob_table,
              blobId,
              storeConfig.getTextColumnName(),
              storeConfig.getEmbeddingColumnName(),
              storeConfig.getMetadataColumnName(),
              chunkConfig,
              embedConfig,
              storeConfig.getMetadata()
      );
  }

  @Override
  public void ensureDocumentsTable(DBNConnection conn, String filesTable) throws SQLException {
    executeUpdate(conn,"ensure-documents-table",filesTable);
  }

  @Override
  public void insertEmptyDocumentRow(DBNConnection conn, String filesTable, String id, String fileMetadata) throws SQLException {
    executeUpdate(conn,"insert-empty-document-row", filesTable, id, fileMetadata);
  }

  @Override
  public void streamContentToBlob(DBNConnection conn, String filesTable, String id, InputStream in) throws SQLException {
    executeUpdate(conn,"stream-file-content-to-blob",filesTable,in,id);
  }

  @Override
  public void createEmbeddingTable(DBNConnection conn, String ownerName, String tableName, String keyColumnName, String textColumnName, String embeddingColumnName, String metadataColumnName) throws SQLException {
    executeUpdate(conn, "create-embedding-table", ownerName, tableName, keyColumnName, textColumnName, embeddingColumnName, metadataColumnName);
  }

  @Override
  public void loadOnnxModelThroughJdbc(String modelName, Blob modelBlob, DBNConnection conn) throws SQLException {
    executeCall(conn,null,"load-onnx-model-through-jdbc",modelName, modelBlob);
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

    @Override
    public void enableDataAccess(DBNConnection connection) throws SQLException {
        executeUpdate(connection, "enable-data-access");
    }

    @Override
    public void disableDataAccess(DBNConnection connection) throws SQLException {
        executeUpdate(connection, "disable-data-access");
    }
}
