package com.dbn.vector.pipeline;

import com.dbn.common.util.Naming;
import com.dbn.common.util.UUIDs;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.vector.model.FileResult;
import com.dbn.vector.model.PipelineStep;
import com.dbn.vector.model.SourceStatus;
import com.dbn.vector.model.StepResult;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.common.FileContent;
import com.dbn.vector.model.sourceconfig.FileSystemSourceConfig;
import com.dbn.vector.service.FileProcessingService;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;


public class FileEmbeddingPipeline extends EmbeddingPipeline {

    private final FileProcessingService fileService = new FileProcessingService();

    @Override
    protected void executeSourceSpecificPipeline(
            @NotNull VectorEmbeddingRequest request,
            @NotNull ConnectionHandler handler,
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull ProgressIndicator progressIndicator,
            @NotNull VectorEmbeddingResult result) throws Exception {

        // ensure documents table exists (shared step for all files)
        StepResult step = result.getstep(PipelineStep.ENSURE_DOCUMENT_TABLE);
        ensureDocumentsTableStep(connection, vectorInterface, step, handler.getUserName());
//        result.addSharedStep(step);

        if (step.getStatus() == StepResult.STEP_STATUS.FAILED && step.isCritical()) {
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

            FileResult fileResult = result.initFileResult(file);

            // Process the file
            processFile(request, connection, vectorInterface, progressIndicator, file, i, fileResult);
        }
    }

    /**
     * Process a single file through the embedding pipeline.
     */
    private void processFile(
            @NotNull VectorEmbeddingRequest request,
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull ProgressIndicator progressIndicator,
            @NotNull VirtualFile file,
            int currentIndex,
            @NotNull FileResult fileResult) {

        int totalFiles = request.getRecordCount();
        String shortenFileName = Naming.shortenFileName(file.getName(), 40);


        try {
            // ========== PHASE 1: Read File Once ==========
            progressIndicator.setText2(
                    String.format("Reading file \"%s\" (%d/%d)",
                            shortenFileName, currentIndex + 1, totalFiles)
            );

            FileContent fileContent;
            try {
                fileContent = new FileContent(file);  // ← READ FILE ONCE HERE
            } catch (IOException | NoSuchAlgorithmException e) {
                fileResult.finishFailed("FILE_READ_ERROR", e.getMessage());
                return;
            }

            progressIndicator.setText2(
                    String.format("Checking file \"%s\" (%d/%d)",
                            shortenFileName, currentIndex + 1, totalFiles)
            );

            String documentId = fileService.checkFileExists(
                    connection,
                    vectorInterface,
                    fileContent,
                    fileResult
            );

            if (!fileResult.getStatus().equals(SourceStatus.RUNNING)) {
                return;  // Check failed
            }

            if (documentId != null) {
                // File already exists - use existing ID
                progressIndicator.setText2(
                        String.format("File already uploaded, using existing \"%s\" (%d/%d)",
                                shortenFileName, currentIndex + 1, totalFiles)
                );

                fileResult.setDocId(documentId);
                fileResult.deleteStep(PipelineStep.UPLOADING_FILE);  // Skip upload

            } else {
                // New file - upload it
                progressIndicator.setText2(
                        String.format("Uploading file \"%s\" (%d/%d)",
                                shortenFileName, currentIndex + 1, totalFiles)
                );

                documentId = UUIDs.compact();
                fileResult.setDocId(documentId);

                fileService.uploadFile(
                        connection,
                        vectorInterface,
                        fileContent,
                        documentId,
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
                    vectorInterface,
                    documentId,  // ← Pass ID only, no bytes!
                    fileContent,
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
