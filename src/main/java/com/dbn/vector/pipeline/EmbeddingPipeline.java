package com.dbn.vector.pipeline;

import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.request.EmbeddingDestinationConfig;
import com.dbn.vector.model.request.EmbeddingStagingConfig;
import com.dbn.vector.model.result.PipelineStep;
import com.dbn.vector.model.result.StepResult;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;


public abstract class EmbeddingPipeline {

    /**
     * Execute the complete embedding pipeline.
     */
    public void execute(
            @NotNull VectorEmbeddingRequest request,
            @NotNull ConnectionHandler handler,
            @NotNull DBNConnection connection,
            @NotNull ProgressIndicator progressIndicator,
            @NotNull VectorEmbeddingResult result) throws Exception {

        DatabaseVectorInterface vectorInterface = handler.getVectorInterface();

        // Step 1: Ensure destination table (shared across all sources)
        StepResult ensureDestStep = result.getstep(PipelineStep.ENSURE_DESTINATION);
        ensureDestinationTableStep(request, ensureDestStep);

        // Step 2: Source-specific preparation and embedding
        executeSourceSpecificPipeline(
                request,
                handler,
                connection,
                vectorInterface,
                progressIndicator,
                result
        );
    }

    /**
     * Execute source-specific embedding logic.
     */
    protected abstract void executeSourceSpecificPipeline(
            @NotNull VectorEmbeddingRequest request,
            @NotNull ConnectionHandler handler,
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull ProgressIndicator progressIndicator,
            @NotNull VectorEmbeddingResult result
    ) throws Exception;

    // ========== Common Steps ==========

    /**
     * Ensure the destination embedding table exists.
     * This is a shared step that applies to all sources.
     */
    protected StepResult ensureDestinationTableStep(
            @NotNull VectorEmbeddingRequest request,
            StepResult step) {

        step.start();
        EmbeddingDestinationConfig destinationConfig = request.getDestinationConfig();
        step.setLink(destinationConfig.getSchemaName()+"."+ destinationConfig.getTableName());
        step.setIcon(Icons.DBO_TABLE);
        step.markSuccess();
        return step;
    }

    /**
     * Ensure the documents metadata table exists (for file sources).
     * This is also a shared step for all file-based sources.
     */
    protected void ensureDocumentsTableStep(
            @NotNull VectorEmbeddingRequest request,
            StepResult step) {

        step.start();

        EmbeddingStagingConfig stagingConfig = request.getStagingConfig();
        String tableIdentifier = stagingConfig.getSchemaName() + "." + stagingConfig.getTableName();
        step.markSuccess();
        step.setLink(tableIdentifier);
        step.setIcon(Icons.DBO_TABLE);
    }
}
