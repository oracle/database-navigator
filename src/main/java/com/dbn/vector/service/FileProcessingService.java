package com.dbn.vector.service;

import com.dbn.common.util.Json;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.request.EmbeddingDestinationConfig;
import com.dbn.vector.model.request.EmbeddingStagingConfig;
import com.dbn.vector.model.result.EmbeddingFileResult;
import com.dbn.vector.model.result.PipelineStep;
import com.dbn.vector.model.result.StepResult;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.dbn.vector.DatabaseVectorManager.ENGINE_VERSION;
import static com.dbn.vector.model.request.EmbeddingSourceType.FILE_SYSTEM;

@Slf4j
public class FileProcessingService {

    public String resolveFileStoreId(
            @NotNull DBNConnection connection,
            @NotNull VectorEmbeddingRequest request,
            @NotNull EmbeddingFileResult result) {

        StepResult step = result.startStep(PipelineStep.CHECK_CRC);
        DatabaseVectorInterface vectorInterface = request.getVectorInterface();
        ResultSet resultSet = null;

        try {
            // Query database: does file with this hash and size exist?
            EmbeddingStagingConfig stagingConfig = request.getStagingConfig();
            resultSet = vectorInterface.loadFileStoreMetadata(
                    connection,
                    stagingConfig.getSchemaName(),
                    stagingConfig.getTableName(),
                    result.getFileHash(),
                    result.getFileSize());

            if (resultSet.next()) {
                String fileStoreId = resultSet.getString("id");
                String metadata = resultSet.getString("metadata");
                result.setMetadata(metadata);
                step.markSuccess();
                return fileStoreId;
            }

            step.markSuccess();

        } catch (Exception e) {
            step.markFailed("CRC_ERROR", e);
            result.finishFailed("CRC_ERROR", e);
        } finally {
            Resources.close(resultSet);
        }
        return null;
    }


    public void uploadFile(
            @NotNull DBNConnection connection,
            @NotNull VectorEmbeddingRequest request,
            @NotNull EmbeddingFileResult result) {

        StepResult step = result.startStep(PipelineStep.UPLOADING_FILE);

        try {
            if (!result.getSource().isUploadAuthorized()) {
                throw new SecurityException("File source upload was not authorized in the current session");
            }

            // Extract metadata from FileContent and connection
            String fileMetadata = buildFileMetadata(connection, result);
            result.setMetadata(fileMetadata);
            EmbeddingStagingConfig stagingConfig = request.getStagingConfig();

            DatabaseVectorInterface vectorInterface = request.getVectorInterface();

            // Step 1: Insert row with metadata and hash
            vectorInterface.createFileStoreEntry(
                    connection,
                    stagingConfig.getSchemaName(),
                    stagingConfig.getTableName(),
                    result.getFileStoreId(),
                    fileMetadata,
                    result.getFileHash(),
                    result.getFileSize());

            // Step 2: Write file bytes to BLOB column
            // NOTE: Pass bytes directly, not JDBC Blob
          try (InputStream inputStream = result.getSource().getFileInputStream()) {
            vectorInterface.uploadFileStoreContent(
                    connection,
                    stagingConfig.getSchemaName(),
                    stagingConfig.getTableName(),
                    result.getFileStoreId(),   // Cached bytes from FileContent
                    inputStream);
          }


            step.markSuccess();
        } catch (Exception e) {
            step.markFailed("UPLOAD_ERROR", e);
            result.finishFailed("UPLOAD_ERROR", e);
        }
    }

    public void embedFile(
            @NotNull DBNConnection connection,
            @NotNull VectorEmbeddingRequest request,
            @NotNull EmbeddingFileResult result) {

        StepResult step = result.startStep(PipelineStep.EMBED);

        try {
            // Check if embeddings already exist for this document
            EmbeddingStagingConfig stagingConfig = request.getStagingConfig();
            EmbeddingDestinationConfig destinationConfig = request.getDestinationConfig();
            DatabaseVectorInterface vectorInterface = request.getVectorInterface();

            boolean alreadyEmbedded = vectorInterface.isContentEmbedded(
                    connection,
                    destinationConfig.getSchemaName(),
                    destinationConfig.getTableName(),
                    result.getFileStoreId());

            if (alreadyEmbedded) {
                step.markSuccess();
                result.finishSkipped();  // 0 new rows - already embedded
                return;
            }

            String rowMetadata = buildRowMetadata(request, Json.readAsMap(result.getMetadata()));
            String chunkConfigJson = request.getChunkConfigJson();
            String embedConfigJson = request.getModelConfigJson();

            // Call embedFileContent with documentId
            // It will SELECT the BLOB from database and process it
            int embeddedRows = vectorInterface.embedFileContent(
                    connection,
                    chunkConfigJson,
                    embedConfigJson,
                    stagingConfig.getSchemaName(),
                    stagingConfig.getTableName(),
                    destinationConfig.getSchemaName(),
                    destinationConfig.getTableName(),
                    result.getFileStoreId(), rowMetadata);

            step.markSuccess();
            result.finishSuccess(embeddedRows);

        } catch (Throwable e) {
            step.markFailed("EMBED_ERROR", e);
            result.finishFailed("EMBED_ERROR", e);
        }
    }


    private String buildFileMetadata(
            @NotNull DBNConnection connection,
            @NotNull EmbeddingFileResult fileResult) throws SQLException {

        VirtualFile file = fileResult.getFile();

        @NonNls
        Map<String, Object> metadata = new LinkedHashMap<>();

        metadata.put("source_id", fileResult.getFileStoreId());
        metadata.put("file_name", file.getName());
        metadata.put("file_path", file.getPath());
        metadata.put("file_size", fileResult.getFileSize());
        metadata.put("upload_timestamp", System.currentTimeMillis());
        metadata.put("uploaded_by", connection.getSchema() != null ? connection.getSchema() : "unknown");
        return Json.writeAsString(metadata);
    }

    private String buildRowMetadata(@NotNull VectorEmbeddingRequest request, Map<String, Object> fileMetadata){
        @NonNls Map<String, Object> sourceMetadata = new LinkedHashMap<>();
        sourceMetadata.put("source_type", FILE_SYSTEM);
        sourceMetadata.putAll(fileMetadata);

        @NonNls Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("engine_version", ENGINE_VERSION);
        metadata.put("embedding_source", sourceMetadata);
        metadata.put("embedding_config", request.getModelConfig().getConfigMap());
        metadata.put("chunking_config", request.getChunkConfig().getConfigMap());
        return Json.writeAsString(metadata);
    }
}
