package com.dbn.vector.service;

import com.dbn.common.util.Json;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.vector.model.FileResult;
import com.dbn.vector.model.PipelineStep;
import com.dbn.vector.model.StepResult;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.common.FileContent;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.dbn.vector.model.sourceconfig.SourceType.FILE_SYSTEM;

@Slf4j
public class FileProcessingService {
    public static final String FILES_TABLE = "document_files";

    public String checkFileExists(
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
                String metadata = rs.getString("metadata");
                fileContent.setMetadata(Json.readAsMap(metadata));
                step.markSuccess();
                return existingDocId;
            }

            step.markSuccess();

        } catch (Exception e) {
            step.markFailed("CRC_ERROR", e.getMessage());
            fileResult.finishFailed("CRC_ERROR", e.getMessage());
        }
        return null;
    }


    public void uploadFile(
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull FileContent fileContent,
            @NotNull String documentId,
            @NotNull FileResult fileResult) {

        StepResult step = fileResult.startStep(PipelineStep.UPLOADING_FILE);

        try {
            // Extract metadata from FileContent and connection
            String metadataJson = buildFileMetadata(connection, fileContent);


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
          try (InputStream inputStream = fileContent.getInputStream()) {
            vectorInterface.writeBlobContent(
                    connection,
                    FILES_TABLE,
                    documentId,
                    inputStream  // Cached bytes from FileContent
            );
          }


            step.markSuccess();
        } catch (Exception e) {
            step.markFailed("UPLOAD_ERROR", e.getMessage());
            fileResult.finishFailed("UPLOAD_ERROR", e.getMessage());
        }
    }

    public void embedFile(
            @NotNull VectorEmbeddingRequest request,
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull String documentId,  // ID only, not bytes!
            FileContent fileContent, @NotNull FileResult fileResult) {

        StepResult step = fileResult.startStep(PipelineStep.EMBED);

        try {
            String rowMetadata = buildRowMetadata(request, fileContent.getMetadata());
            String chunkConfigJson = request.getChunkConfig().getConfigJson();
            String embedConfigJson = request.getEmbedConfig().getConfigJson();

            // Call embedFileContent with documentId
            // It will SELECT the BLOB from database and process it
            int embeddedRows = vectorInterface.embedFileContent(
                    connection,
                    chunkConfigJson,
                    embedConfigJson,
                    request.getStoreConfig(),
                    documentId,
                    rowMetadata
            );

            step.markSuccess();
            fileResult.finishSuccess(embeddedRows);

        } catch (Throwable e) {
            step.markFailed("EMBED_ERROR", e.getMessage());
            fileResult.finishFailed("EMBED_ERROR", e.getMessage());
        }
    }


    private String buildFileMetadata(
            @NotNull DBNConnection connection,
            @NotNull FileContent fileContent) throws SQLException {

        VirtualFile file = fileContent.getFile();

        @NonNls
        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("file_name", file.getName());
        metadata.put("file_path", file.getPath());
        metadata.put("file_size", fileContent.getFileSize());
        metadata.put("primary_key", fileContent.getId());
        metadata.put("upload_timestamp", System.currentTimeMillis());
        metadata.put("uploaded_by", connection.getSchema() != null ? connection.getSchema() : "unknown");

        fileContent.setMetadata(metadata);
        return Json.writeAsString(metadata);
    }

    private String buildRowMetadata(@NotNull VectorEmbeddingRequest request, Map<String, Object> fileMetadata){
        Map<String, Object> sourceMetadata = new LinkedHashMap<>();
        sourceMetadata.put("source_type", FILE_SYSTEM);
        sourceMetadata.putAll(fileMetadata);

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("embedding_source", sourceMetadata);
        metadata.put("embedding_config", request.getEmbedConfig().getConfigMap());
        metadata.put("chunking_config", request.getChunkConfig().getConfigMap());
        return Json.writeAsString(metadata);
    }
}
