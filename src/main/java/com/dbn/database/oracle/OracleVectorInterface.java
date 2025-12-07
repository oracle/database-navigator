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

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.DatabaseInterfaceBase;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.object.factory.ModelFactoryInput;
import com.dbn.vector.model.sourceconfig.DBTableSourceConfig;
import com.dbn.vector.model.store.StoreConfig;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.vector.service.FileProcessingService.FILES_TABLE;

@Slf4j
public class OracleVectorInterface extends DatabaseInterfaceBase implements DatabaseVectorInterface {

  public OracleVectorInterface(DatabaseInterfaces provider) {
    super("oracle_vector_interface.xml", provider);
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
  public int embedDataContent(DBNConnection conn, DBTableSourceConfig sourceConfig, String chunkConfig, String embedConfig, StoreConfig storeConfig, @NotNull String metadata) throws SQLException {
    return executeUpdate(conn,
            "insert-vector-embeddings-from-table",
            storeConfig.getSchemaName(),
            storeConfig.getTableName(),
            storeConfig.getTextColumnName(),
            storeConfig.getEmbeddingColumnName(),
            storeConfig.getMetadataColumnName(),
            sourceConfig.getSchemaName(),
            sourceConfig.getTableName(),
            sourceConfig.getKeyColumnName(),
            sourceConfig.getDataColumnName(),
            chunkConfig,
            embedConfig,
            metadata
    );
  }

  @Override
  public int embedFileContent(DBNConnection conn, String chunkConfig, String embedConfig, StoreConfig storeConfig, String documentId, String metadata) throws SQLException {
      return executeUpdate(conn,
              "insert-vector-embeddings-from-filesystem",
              storeConfig.getSchemaName(),
              storeConfig.getTableName(),
              storeConfig.getTextColumnName(),
              storeConfig.getEmbeddingColumnName(),
              storeConfig.getMetadataColumnName(),
              FILES_TABLE,
              documentId, // id of the blob
              chunkConfig,
              embedConfig,
              metadata);
  }

  @Override
  public void writeBlobContent(@NotNull DBNConnection conn, String filesTable, @NotNull String documentId, InputStream inputStream) throws SQLException {
    executeUpdate(conn,"stream-file-content-to-blob",filesTable, inputStream,documentId);
  }

  @Override
  public void ensureDocumentsTable(DBNConnection conn, String schemaName, String tableName) throws SQLException {
    executeUpdate(conn,"ensure-documents-table", schemaName, tableName);
  }

  @Override
  public void insertEmptyDocumentRow(DBNConnection conn, String filesTable, String id, String fileMetadata, String fileHash, long fileSize) throws SQLException {
    executeUpdate(conn,"insert-empty-document-row", filesTable, id, fileMetadata, fileHash,fileSize);
  }

  @Override
  public ResultSet selectDocumentIdByHashIfExists(DBNConnection conn, String filesTable, String hash, long filesize) throws SQLException {
    return executeQuery(conn,"select-document-id-by-hash",filesTable,hash,filesize);
  }


  @Override
  public void createEmbeddingTable(DBNConnection conn, String ownerName, String tableName, String keyColumnName, String textColumnName, String embeddingColumnName, String metadataColumnName) throws SQLException {
    executeUpdate(conn, "create-embedding-table", ownerName, tableName, keyColumnName, textColumnName, embeddingColumnName, metadataColumnName);
  }

  @Override
  public void loadOnnxModelThroughJdbc(String modelName, Blob modelBlob, DBNConnection conn) throws SQLException {
    executeCall(conn,null,"load-onnx-model-through-jdbc",modelName, modelBlob);
  }

}
