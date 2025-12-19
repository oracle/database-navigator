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

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.object.factory.model.DBAIModelFactoryInput;
import com.dbn.vector.model.source.DBTableSourceConfig;
import com.dbn.vector.model.store.StoreConfig;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;

public interface DatabaseVectorInterface extends DatabaseInterface {

  void loadOnnxModelFromOci(DBAIModelFactoryInput input, DBNConnection conn) throws SQLException;

  void deleteAIModel(DBNConnection conn,String modelName) throws SQLException;

  void loadOnnxModelThroughJdbc(String modelName, Blob modelBlob, DBNConnection conn) throws SQLException;

  ResultSet chunkTextContent(String text, String chunkBy, String splitBy, int max, int overlap, DBNConnection conn) throws SQLException;

  void createEmbeddingTable(DBNConnection connection, String ownerName, String tableName, String keyColumnName, String textColumnName, String embeddingColumnName, String metadataColumnName) throws SQLException;

  int embedDataContent(DBNConnection connection, DBTableSourceConfig sourceConfig, String chunkConfig, String embedConfig, StoreConfig storeConfig, @NotNull String metadata) throws SQLException;

  int embedDataContent(DBNConnection connection, DBTableSourceConfig sourceConfig, String chunkConfig, String embedConfig, StoreConfig storeConfig, @NotNull String metadata, int batchSize) throws SQLException;

  int embedFileContent(DBNConnection conn, String chunkConfig, String embedConfig, StoreConfig storeConfig, String documentId, String metadata) throws SQLException;

  void ensureFileStoreTable(DBNConnection conn, String schemaName, String tableName) throws SQLException;

  void createFileStoreEntry(DBNConnection conn, String filesTable, String id, String fileMetadata, String fileHash, long fileSize) throws SQLException;

  ResultSet loadFileStoreMetadata(DBNConnection conn, String filesTable, String crc, long filesize) throws SQLException;

  void uploadFileStoreContent(@NotNull DBNConnection connection, String filesTable, @NotNull String documentId, InputStream inputStream) throws SQLException;

  boolean isContentEmbedded(DBNConnection conn, String schemaName, String tableName, String metadataColumnName, String sourceId) throws SQLException;
}
