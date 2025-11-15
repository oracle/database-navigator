package com.dbn.vector.service;

import com.dbn.common.util.Json;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.vector.model.FileResult;
import com.dbn.vector.model.PipelineStep;
import com.dbn.vector.model.SourceStatus;
import com.dbn.vector.model.StepResult;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.store.StoreConfig;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.zip.CRC32;

@Slf4j
public class FileProcessingService {
    private static final String FILES_TABLE = "document_files";
    private static final int BUFFER_SIZE = 64 * 1024;

    public String generateDocumentId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public long checkFileExists(@NotNull DBNConnection connection, @NotNull DatabaseAssistantInterface assistantInterface, @NotNull VirtualFile file, @NotNull FileResult fileResult) {
        StepResult step = fileResult.startStep(PipelineStep.CHECK_CRC);
        try {
            long crc = computeCRC(file);
            
            // Try to get existing blob by CRC - if it exists, we'll reuse it
            ResultSet rs = assistantInterface.selectBlobByCRC(connection, FILES_TABLE, crc);
            
            if (rs.next()) {
                // File exists! Get the existing document ID and blob
                String existingDocId = rs.getString("id");
                Blob existingBlob = rs.getBlob("file_content");
                
                fileResult.setExisted(true);
                fileResult.setDocId(existingDocId);
                fileResult.setCachedBlob(existingBlob); // Store for later use
            }

            step.markSuccess();
            return crc;
            
        } catch (Exception e) {
            step.markFailed("CRC_ERROR", e.getMessage());
            fileResult.finishFailed("CRC_ERROR", e.getMessage());
            return 0;
        }
    }

    public Blob uploadFile(@NotNull DBNConnection connection, @NotNull DatabaseAssistantInterface assistantInterface, @NotNull VirtualFile file, @NotNull String documentId, long crc, @NotNull FileResult fileResult) {
        StepResult step = fileResult.startStep(PipelineStep.UPLOADING_FILE);
        try {
            Map<String, Object> metadata = extractFileMetadata(connection, file);
            String metadataJson = Json.writeAsString(metadata);
            assistantInterface.insertEmptyDocumentRow(connection, FILES_TABLE, documentId, metadataJson, crc);
            Blob blob = writeFileToBlob(connection, assistantInterface, documentId, file);
            step.markSuccess();
            return blob;
        } catch (Exception e) {
            step.markFailed("UPLOAD_ERROR", e.getMessage());
            fileResult.finishFailed("UPLOAD_ERROR", e.getMessage());
            return null;
        }
    }

    /**
     * Get the existing blob that was retrieved during CRC check.
     */
    public Blob getExistingBlob(@NotNull FileResult fileResult) {
        return fileResult.getCachedBlob();
    }

    public void embedFile(@NotNull VectorEmbeddingRequest request, @NotNull DBNConnection connection, @NotNull DatabaseAssistantInterface assistantInterface, @NotNull VirtualFile file, @NotNull String documentId, @NotNull Blob blobData, @NotNull FileResult fileResult) {
        StepResult step = fileResult.startStep(PipelineStep.EMBED);
        try {
            String rowMetadata = buildRowMetadata(request, documentId, connection, file);
            String chunkConfigJson = request.getChunkConfig().getConfigJson();
            String embedConfigJson = request.getEmbedConfig().getConfigJson();
            int embeddedRows = assistantInterface.embedFileContent(connection, chunkConfigJson, embedConfigJson, request.getStoreConfig(), blobData, rowMetadata);
            step.markSuccess();
            fileResult.finishSuccess(embeddedRows);
        } catch (SQLException | IOException e) {
            step.markFailed("EMBED_ERROR", e.getMessage());
            fileResult.finishFailed("EMBED_ERROR", e.getMessage());
        }
    }

    private long computeCRC(@NotNull VirtualFile file) throws IOException {
        CRC32 crc = new CRC32();
        try (InputStream in = file.getInputStream()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                crc.update(buffer, 0, bytesRead);
            }
        }
        return crc.getValue();
    }

    private Map<String, Object> extractFileMetadata(@NotNull DBNConnection connection, @NotNull VirtualFile file) throws IOException, SQLException {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("filename", file.getName());
        metadata.put("path", file.getPath());
        metadata.put("size_bytes", file.getLength());
        metadata.put("uploaded_by", connection.getSchema() != null ? connection.getSchema() : "unknown");
        metadata.put("uploaded_at", Instant.now().toString());
        return metadata;
    }

    private Blob writeFileToBlob(@NotNull DBNConnection connection, @NotNull DatabaseAssistantInterface assistantInterface, @NotNull String documentId, @NotNull VirtualFile file) throws SQLException, IOException {
        ResultSet rs = assistantInterface.selectEmptyBlob(connection, FILES_TABLE, documentId);
        if (!rs.next()) {
            throw new SQLException("No row found in " + FILES_TABLE + " for id=" + documentId);
        }
        Blob blob = rs.getBlob(1);
        try (InputStream in = file.getInputStream(); OutputStream out = blob.setBinaryStream(1)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            out.flush();
        }
        return blob;
    }

    private String buildRowMetadata(@NotNull VectorEmbeddingRequest request, @NotNull String documentId, @NotNull DBNConnection connection, @NotNull VirtualFile file) throws IOException, SQLException {
        Map<String, Object> metadata = extractFileMetadata(connection, file);
        metadata.put("doc_id", documentId);
        metadata.put("embed_config", request.getEmbedConfig().getConfigJson());
        metadata.put("chunk_config", request.getChunkConfig().getConfigJson());
        return Json.writeAsString(metadata);
    }
}
