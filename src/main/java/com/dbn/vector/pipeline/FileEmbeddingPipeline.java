package com.dbn.vector.pipeline;

import com.dbn.common.task.TaskStatus;
import com.dbn.common.util.UUIDs;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.vector.model.VectorEmbeddingContext;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.request.EmbeddingFileSource;
import com.dbn.vector.model.request.EmbeddingSourceFiles;
import com.dbn.vector.model.result.EmbeddingFileResult;
import com.dbn.vector.model.result.PipelineStep;
import com.dbn.vector.service.FileProcessingService;
import org.jetbrains.annotations.NotNull;

import java.util.List;



public class FileEmbeddingPipeline implements EmbeddingPipeline {

    private final FileProcessingService fileService = new FileProcessingService();

    @Override
    public void execute(@NotNull VectorEmbeddingContext context) {
        VectorEmbeddingRequest request = context.getRequest();
        VectorEmbeddingResult result = context.getResult();

        // Process each file individually
        EmbeddingSourceFiles fileConfig = request.getSourceConfig().getSourceFiles();
        List<EmbeddingFileSource> sources = fileConfig.getElements();
        for (EmbeddingFileSource source : sources) {
            if (context.isCancellationRequested()) break;
            EmbeddingFileResult fileResult = result.getResult(source);
            processFile(context, request, fileResult);
        }
    }

    /**
     * Process a single file through the embedding pipeline.
     */
    private void processFile(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull EmbeddingFileResult result) {

        DBNConnection connection = context.getConnection();

        try {
            // ========== PHASE 1: Read File Once ==========
            try {
                result.initSource();
            } catch (Exception e) {
                result.finishFailed("FILE_READ_ERROR", e);
                return;
            }

            String fileStoreId = fileService.resolveFileStoreId(
                    connection,
                    request,
                    result);

            if (!result.getStatus().equals(TaskStatus.RUNNING)) {
                return;  // Check failed
            }

            if (fileStoreId != null) {
                // File already exists - use existing ID
                result.setFileStoreId(fileStoreId);
                result.deleteStep(PipelineStep.UPLOADING_FILE);  // Skip upload

            } else {
                // New file - upload it
                fileStoreId = UUIDs.compact();
                result.setFileStoreId(fileStoreId);
                fileService.uploadFile(
                        connection,
                        request,
                        result);
            }

            if (!result.getStatus().equals(TaskStatus.RUNNING)) {
                return; // Upload failed
            }

            // Step 3: Embed file (always execute, whether file was uploaded or already existed)
            fileService.embedFile(
                    connection,
                    request,
                    result);
        } catch (Exception e) {
            result.finishFailed("UNEXPECTED_ERROR", e);
        }
    }
}
