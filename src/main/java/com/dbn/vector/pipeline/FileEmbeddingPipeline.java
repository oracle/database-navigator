package com.dbn.vector.pipeline;

import com.dbn.common.task.TaskStatus;
import com.dbn.common.util.Naming;
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
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;


public class FileEmbeddingPipeline implements EmbeddingPipeline {

    private final FileProcessingService fileService = new FileProcessingService();

    @Override
    public void execute(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull VectorEmbeddingResult result) {

        ProgressIndicator progressIndicator = context.getProgressIndicator();
        // Process each file individually
        EmbeddingSourceFiles fileConfig = request.getSourceConfig().getSourceFiles();
        List<EmbeddingFileSource> sources = fileConfig.getElements();
        for (int i = 0; i < sources.size(); i++) {
            EmbeddingFileSource source = sources.get(i);

            progressIndicator.setText2(txt("prc.vector.text.ProcessingFile", source.getFileName(), i + 1, sources.size()));
            EmbeddingFileResult fileResult = result.getResult(source);
            processFile(context, request, fileResult, i);
        }
    }

    /**
     * Process a single file through the embedding pipeline.
     */
    private void processFile(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull EmbeddingFileResult result,
            int currentIndex) {

        VirtualFile file = result.getFile();

        int totalFiles = request.getRecordCount();
        String shortenFileName = Naming.shortenFileName(file.getName(), 40);
        ProgressIndicator progressIndicator = context.getProgressIndicator();
        DBNConnection connection = context.getConnection();

        try {
            // ========== PHASE 1: Read File Once ==========
            progressIndicator.setText2(txt("prc.vector.text.ReadingFile", shortenFileName, currentIndex + 1, totalFiles));
            try {
                result.initSource();
            } catch (Exception e) {
                result.finishFailed("FILE_READ_ERROR", e);
                return;
            }

            progressIndicator.setText2(txt("prc.vector.text.CheckingFile", shortenFileName, currentIndex + 1, totalFiles));

            String fileStoreId = fileService.resolveFileStoreId(
                    connection,
                    request,
                    result);

            if (!result.getStatus().equals(TaskStatus.RUNNING)) {
                return;  // Check failed
            }

            if (fileStoreId != null) {
                // File already exists - use existing ID
                progressIndicator.setText2(txt("prc.vector.text.UsingExistingFile", shortenFileName, currentIndex + 1, totalFiles));

                result.setFileStoreId(fileStoreId);
                result.deleteStep(PipelineStep.UPLOADING_FILE);  // Skip upload

            } else {
                // New file - upload it
                progressIndicator.setText2(txt("prc.vector.text.UploadingFile", shortenFileName, currentIndex + 1, totalFiles));

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
            progressIndicator.setText2(txt("prc.vector.text.EmbeddingFile", shortenFileName, currentIndex + 1, totalFiles));

            fileService.embedFile(
                    connection,
                    request,
                    result);
        } catch (Exception e) {
            result.finishFailed("UNEXPECTED_ERROR", e);
        }
    }
}
