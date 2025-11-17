package com.dbn.vector.pipeline;

import com.dbn.common.util.Naming;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.vector.model.*;
import com.dbn.vector.model.sourceconfig.FileSystemSourceConfig;
import com.dbn.vector.service.FileProcessingService;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class FileEmbeddingPipeline extends EmbeddingPipeline {

    private final FileProcessingService fileService = new FileProcessingService();

    @Override
    protected void executeSourceSpecificPipeline(
            @NotNull VectorEmbeddingRequest request,
            @NotNull ConnectionHandler handler,
            @NotNull DBNConnection connection,
            @NotNull DatabaseAssistantInterface assistantInterface,
            @NotNull ProgressIndicator progressIndicator,
            @NotNull VectorEmbeddingResult result,
            @NotNull StepResult ensureDestStep) throws Exception {

        // ensure documents table exists (shared step for all files)
        StepResult ensureDocumentStep = ensureDocumentsTableStep(connection, assistantInterface);
        result.addSharedStep(ensureDocumentStep);

        if (ensureDocumentStep.getStatus() == StepResult.STEP_STATUS.FAILED && ensureDocumentStep.isCritical()) {
            return;
        }

        // Process each file individually
        FileSystemSourceConfig fileConfig = request.getSourceConfig().getFileSourceConfig();
        List<VirtualFile> files = fileConfig.getFiles();

        for (int i = 0; i < files.size(); i++) {
            VirtualFile file = files.get(i);

            progressIndicator.setText2(
                    String.format("Processing file \"%s\" (%d/%d)", file.getName(), i + 1, files.size())
            );

            FileResult fileResult = result.ensureFileResult(file);

            // Add shared steps to this file result
            fileResult.getSteps().add(ensureDestStep);
            fileResult.getSteps().add(ensureDocumentStep);

            // Process the file
            processFile(request, connection, assistantInterface, progressIndicator, file, i, files.size(), fileResult);
        }
    }

    /**
     * Process a single file through the embedding pipeline.
     */
    private void processFile(
            @NotNull VectorEmbeddingRequest request,
            @NotNull DBNConnection connection,
            @NotNull DatabaseAssistantInterface assistantInterface,
            @NotNull ProgressIndicator progressIndicator,
            @NotNull VirtualFile file,
            int currentIndex,
            int totalFiles,
            @NotNull FileResult fileResult) {
        String shortenFileName = Naming.shortenFileName(file.getName(), 40);


        try {
            String documentId = fileService.generateDocumentId();
            fileResult.setDocId(documentId);

            // Step 1: Check CRC
            progressIndicator.setText2(
                    String.format("Checking file \"%s\" (%d/%d)", shortenFileName
                            , currentIndex + 1, totalFiles)
            );

            long crc = fileService.checkFileExists(connection, assistantInterface, file, fileResult);

            if (!fileResult.getStatus().equals(SourceStatus.RUNNING)) {
                return; // CRC check failed
            }

            java.sql.Blob blobData = null;
            // Step 2: Upload file (only if it doesn't already exist)
            if (fileResult.isExisted()) {
                // File already exists, use cached blob from CRC check
                progressIndicator.setText2(
                        String.format("File already uploaded, using existing \"%s\" (%d/%d)",
                                shortenFileName, currentIndex + 1, totalFiles)
                );
                
                blobData = fileService.getExistingBlob(fileResult);
                
            } else {
                // File doesn't exist, upload it
                progressIndicator.setText2(
                        String.format("Uploading file \"%s\" (%d/%d)", shortenFileName
                                , currentIndex + 1, totalFiles)
                );

                blobData = fileService.uploadFile(
                        connection,
                        assistantInterface,
                        file,
                        documentId,
                        crc,
                        fileResult
                );
            }

            if (!fileResult.getStatus().equals(SourceStatus.RUNNING)) {
                return; // Upload failed
            }

            // Step 3: Embed file (always execute, whether file was uploaded or already existed)
            progressIndicator.setText2(
                    String.format("Embedding file \"%s\" (%d/%d)", shortenFileName, currentIndex + 1, totalFiles)
            );

            fileService.embedFile(
                    request,
                    connection,
                    assistantInterface,
                    file,
                    documentId,
                    blobData,
                    fileResult
            );

            // Add visual indicator if file was reused
            if (fileResult.isExisted() && fileResult.getStatus() == SourceStatus.SUCCESS) {
                fileResult.setDisplayName(file.getName() + " (reused)");
            }

        } catch (Exception e) {
            fileResult.finishFailed("UNEXPECTED_ERROR", e.getMessage());
        }
    }
}
