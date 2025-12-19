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
import com.dbn.vector.model.source.DBTableSourceConfig;
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
    public void createModelFromStorage(DBNConnection conn, String ownerName, String modelName, String modelLocation, String credentialName) throws SQLException {
        executeUpdate(conn, "create-model-from-storage", ownerName + "." + modelName, credentialName, modelLocation);
    }

    public void createModelFromFile(DBNConnection conn, String ownerName, String modelName, Blob modelBlob) throws SQLException {
        executeUpdate(conn, "create-model-from-file", ownerName + "." + modelName, modelBlob);
    }

    @Override
    public void dropModel(DBNConnection conn, String ownerName, String modelName) throws SQLException {
        executeUpdate(conn, "drop-model", ownerName + "." + modelName);
    }

    @Override
    public ResultSet chunkTextContent(String text, String chunkBy, String splitBy, int max, int overlap, DBNConnection conn) throws SQLException {
        return executeQuery(conn, "chunk-text-from-chunk-lab", text, chunkBy, max, overlap, splitBy);
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
    public int embedDataContent(DBNConnection conn, DBTableSourceConfig sourceConfig, String chunkConfig, String embedConfig, StoreConfig storeConfig, @NotNull String metadata, int batchSize) throws SQLException {
        return executeUpdate(conn,
                "insert-vector-embeddings-from-table-batch",
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
                metadata,
                batchSize
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
    public void uploadFileStoreContent(@NotNull DBNConnection conn, String filesTable, @NotNull String documentId, InputStream inputStream) throws SQLException {
        executeUpdate(conn, "upload_file_store_content", filesTable, inputStream, documentId);
    }

    @Override
    public void ensureFileStoreTable(DBNConnection conn, String schemaName, String tableName) throws SQLException {
        executeUpdate(conn, "ensure-file-store-table", schemaName, tableName);
    }

    @Override
    public void createFileStoreEntry(DBNConnection conn, String filesTable, String id, String fileMetadata, String fileHash, long fileSize) throws SQLException {
        executeUpdate(conn, "create-file-store-entry", filesTable, id, fileMetadata, fileHash, fileSize);
    }

    @Override
    public ResultSet loadFileStoreMetadata(DBNConnection conn, String filesTable, String fileHash, long filesize) throws SQLException {
        return executeQuery(conn, "load-file-store-metadata", filesTable, fileHash, filesize);
    }

    @Override
    public boolean isContentEmbedded(DBNConnection conn, String schemaName, String tableName, String metadataColumnName, String sourceId) throws SQLException {
        return getBooleanValue(conn, "is-content-embedded", schemaName, tableName, metadataColumnName, sourceId);
    }


    @Override
    public void createEmbeddingTable(DBNConnection conn, String ownerName, String tableName, String keyColumnName, String textColumnName, String embeddingColumnName, String metadataColumnName) throws SQLException {
        executeUpdate(conn, "create-embedding-table", ownerName, tableName, keyColumnName, textColumnName, embeddingColumnName, metadataColumnName);
    }
}
