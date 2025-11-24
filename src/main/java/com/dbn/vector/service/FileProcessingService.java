package com.dbn.vector.service;

import com.dbn.common.util.Json;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.vector.model.FileResult;
import com.dbn.vector.model.PipelineStep;
import com.dbn.vector.model.StepResult;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.common.DuplicateInfo;
import com.dbn.vector.model.common.FileContent;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class FileProcessingService {
    public static final String FILES_TABLE = "document_files";

    public String generateDocumentId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    public DuplicateInfo checkFileExists(
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull FileContent fileContent,
            @NotNull FileResult fileResult) {

        StepResult step = fileResult.startStep(PipelineStep.CHECK_CRC);

        try {
            String fileHash = fileContent.getHash();
            long fileSize = fileContent.getFileSize();

            // Query database: does file with this hash and size exist?
            ResultSet rs = vectorInterface.selectDocumentIdByHashIfExists(
                    connection,
                    FILES_TABLE,
                    fileHash,
                    fileSize
            );

            if (rs.next()) {
                String existingDocId = rs.getString("id");
                step.markSuccess();
                return DuplicateInfo.found(existingDocId);
            }

            step.markSuccess();
            return DuplicateInfo.notFound();

        } catch (Exception e) {
            step.markFailed("CRC_ERROR", e.getMessage());
            fileResult.finishFailed("CRC_ERROR", e.getMessage());
            return DuplicateInfo.notFound();
        }
    }


    public String uploadFile(
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull FileContent fileContent,
            @NotNull String documentId,
            @NotNull FileResult fileResult) {

        StepResult step = fileResult.startStep(PipelineStep.UPLOADING_FILE);

        try {
            // Extract metadata from FileContent and connection
            Map<String, Object> metadata = extractFileMetadata(connection, fileContent);
            String metadataJson = Json.writeAsString(metadata);

            // Step 1: Insert row with metadata and hash
            vectorInterface.insertEmptyDocumentRow(
                    connection,
                    FILES_TABLE,
                    documentId,
                    metadataJson,
                    fileContent.getHash(),  // From FileContent (no re-computation)
                    fileContent.getFileSize()   // From FileContent (no re-reading)
            );

            // Step 2: Write file bytes to BLOB column
            // NOTE: Pass bytes directly, not JDBC Blob
            vectorInterface.writeBlobContent(
                    connection,
                    FILES_TABLE,
                    documentId,
                    fileContent.getBytes()  // Cached bytes from FileContent
            );

            step.markSuccess();
            return documentId;  // Return ID only, not bytes!

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

    public void embedFile(
            @NotNull VectorEmbeddingRequest request,
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull String documentId,  // ID only, not bytes!
            FileContent fileContent, @NotNull FileResult fileResult) {

        StepResult step = fileResult.startStep(PipelineStep.EMBED);

        try {
            String rowMetadata = buildRowMetadata(request, documentId, connection, fileContent);
            String chunkConfigJson = request.getChunkConfig().getConfigJson();
            String embedConfigJson = request.getEmbedConfig().getConfigJson();

            // Call embedFileContent with documentId
            // It will SELECT the BLOB from database and process it
            int embeddedRows = vectorInterface.embedFileContent(
                    connection,
                    chunkConfigJson,
                    embedConfigJson,
                    request.getStoreConfig(),
                    documentId,              // ← ID instead of Blob!
                    rowMetadata
            );

            step.markSuccess();
            fileResult.finishSuccess(embeddedRows);

        } catch (Throwable e) {
            step.markFailed("EMBED_ERROR", e.getMessage());
            fileResult.finishFailed("EMBED_ERROR", e.getMessage());
        }
    }


    private Map<String, Object> extractFileMetadata(
            @NotNull DBNConnection connection,
            @NotNull FileContent fileContent) throws SQLException {

        Map<String, Object> metadata = new HashMap<>();
        VirtualFile file = fileContent.getFile();

        metadata.put("filename", file.getName());
        metadata.put("path", file.getPath());
        metadata.put("size_bytes", fileContent.getFileSize());  // From cache
        metadata.put("uploaded_by",
                connection.getSchema() != null ? connection.getSchema() : "unknown");
        metadata.put("uploaded_at", Instant.now().toString());

        return metadata;
    }



    private String buildRowMetadata(@NotNull VectorEmbeddingRequest request, @NotNull String documentId, @NotNull DBNConnection connection, FileContent file) throws IOException, SQLException {
        Map<String, Object> metadata = extractFileMetadata(connection, file);
        metadata.put("doc_id", documentId);
        metadata.put("embed_config", request.getEmbedConfig().getConfigMap());
        metadata.put("chunk_config", request.getChunkConfig().getConfigMap());
        return Json.writeAsString(metadata);
    }
}
