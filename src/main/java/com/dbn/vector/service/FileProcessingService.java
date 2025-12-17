package com.dbn.vector.service;

import com.dbn.common.util.Json;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.vector.model.FileResult;
import com.dbn.vector.model.PipelineStep;
import com.dbn.vector.model.StepResult;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.common.FileContent;
import com.dbn.vector.model.store.StoreConfig;
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
import static com.dbn.vector.model.sourceconfig.SourceType.FILE_SYSTEM;

@Slf4j
public class FileProcessingService {
    public static final String FILES_TABLE = "document_files";

    public String resolveFileStoreId(
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull FileContent fileContent,
            @NotNull FileResult fileResult) {

        StepResult step = fileResult.startStep(PipelineStep.CHECK_CRC);

        try {
            // Query database: does file with this hash and size exist?
            ResultSet rs = vectorInterface.loadFileStoreMetadata(
                    connection,
                    FILES_TABLE,
                    fileContent.getFileHash(),
                    fileContent.getFileSize());

            if (rs.next()) {
                String fileStoreId = rs.getString("id");
                String metadata = rs.getString("metadata");
                fileContent.setMetadata(Json.readAsMap(metadata));
                step.markSuccess();
                return fileStoreId;
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
            @NotNull String fileStoreId,
            @NotNull FileResult fileResult) {

        StepResult step = fileResult.startStep(PipelineStep.UPLOADING_FILE);

        try {
            // Extract metadata from FileContent and connection
            String metadataJson = buildFileMetadata(connection, fileContent);


            // Step 1: Insert row with metadata and hash
            vectorInterface.createFileStoreEntry(
                    connection,
                    FILES_TABLE,
                    fileStoreId,
                    metadataJson,
                    fileContent.getFileHash(),  // From FileContent (no re-computation)
                    fileContent.getFileSize()   // From FileContent (no re-reading)
            );

            // Step 2: Write file bytes to BLOB column
            // NOTE: Pass bytes directly, not JDBC Blob
          try (InputStream inputStream = fileContent.getInputStream()) {
            vectorInterface.uploadFileStoreContent(
                    connection,
                    FILES_TABLE,
                    fileStoreId,
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
            // Check if embeddings already exist for this document
            StoreConfig storeConfig = request.getStoreConfig();

            boolean alreadyEmbedded = vectorInterface.isContentEmbedded(
                    connection,
                    storeConfig.getSchemaName(),
                    storeConfig.getTableName(),
                    storeConfig.getMetadataColumnName(),
                    documentId
            );

            if (alreadyEmbedded) {
                step.markSuccess();
                fileResult.setExisted(true);
                fileResult.finishSuccess(0);  // 0 new rows - already existed
                return;
            }

            String rowMetadata = buildRowMetadata(request, fileContent.getMetadata());
            String chunkConfigJson = request.getChunkConfig().getConfigJson();
            String embedConfigJson = request.getEmbedConfig().getConfigJson();

            // Call embedFileContent with documentId
            // It will SELECT the BLOB from database and process it
            int embeddedRows = vectorInterface.embedFileContent(
                    connection,
                    chunkConfigJson,
                    embedConfigJson,
                    storeConfig,
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

        metadata.put("source_id", fileContent.getFileStoreId());
        metadata.put("file_name", file.getName());
        metadata.put("file_path", file.getPath());
        metadata.put("file_size", fileContent.getFileSize());
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
        metadata.put("engine_version", ENGINE_VERSION);
        metadata.put("embedding_source", sourceMetadata);
        metadata.put("embedding_config", request.getEmbedConfig().getConfigMap());
        metadata.put("chunking_config", request.getChunkConfig().getConfigMap());
        return Json.writeAsString(metadata);
    }
}
