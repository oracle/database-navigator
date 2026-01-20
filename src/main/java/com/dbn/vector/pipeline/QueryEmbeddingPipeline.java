package com.dbn.vector.pipeline;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.vector.model.VectorEmbeddingContext;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.request.EmbeddingQuerySource;
import com.dbn.vector.model.request.EmbeddingQuerySources;
import com.dbn.vector.model.result.EmbeddingQueryResult;
import com.dbn.vector.model.result.PipelineStep;
import com.dbn.vector.model.result.StepResult;
import com.dbn.vector.service.QueryProcessingService;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.vector.model.result.PipelineStep.ENSURE_DOCUMENT_TABLE;


public class QueryEmbeddingPipeline extends EmbeddingPipeline {
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final QueryProcessingService queryProcessingService = new QueryProcessingService();

    @Override
    protected void executeSourceSpecificPipeline(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull VectorEmbeddingResult result) throws Exception {

        // remove the PREPARE_DOCUMENT_STORE step from shared steps
        result.deleteStepFfromShared(ENSURE_DOCUMENT_TABLE);
        EmbeddingQuerySources sources = request.getSourceConfig().getSourceQueries();

        for (EmbeddingQuerySource source : sources.getElements()) {
            EmbeddingQueryResult queryResult = result.getResult(source);

            String metadata = queryProcessingService.buildRowMetadata(request, source);
            queryResult.setMetadata(metadata);
            context.getProgressIndicator().setText2("Processing table " + queryResult.getName());

            // Execute the embedding with batching
            embedQueryDataInBatches(
                    context, request,
                    queryResult
            );
        }

    }

    /**
     * Embed data from the source table using batching for failure recovery.
     * Each batch is committed separately, so progress is preserved on failure.
     */
    private void embedQueryDataInBatches(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull EmbeddingQueryResult result) throws SQLException {

        StepResult embedStep = result.startStep(PipelineStep.EMBED);
        DBNConnection connection = context.getConnection();
        ProgressIndicator progressIndicator = context.getProgressIndicator();

        try {
            int totalProcessed = 0;
            int batchCount;
            int batchNumber = 0;

            connection.setAutoCommit(false);
            do {
                // Check for cancellation
                if (progressIndicator.isCanceled()) {
                    break;
                }

                batchNumber++;
                progressIndicator.setText2("Processing query " + result.getName() + " (batch " + batchNumber + " / rows embedded " + totalProcessed + ")");

                // Process one batch
                DatabaseVectorInterface vectorInterface = request.getConnection().getVectorInterface();
                batchCount = vectorInterface.embedQueryContent(
                        connection,
                        result.getSelectStatement(),
                        request.getChunkConfig().getConfigJson(),
                        request.getModelConfig().getConfigJson(),
                        request.getDestinationConfig(),
                        result.getMetadata(),
                        DEFAULT_BATCH_SIZE
                );

                // Commit after each batch - this is the recovery point
                connection.commit();

                totalProcessed += batchCount;

            } while (batchCount > 0);

            if (progressIndicator.isCanceled()) {
                embedStep.markSuccess();
                result.finishSuccess(totalProcessed);
            } else {
                embedStep.markSuccess();
                result.finishSuccess(totalProcessed);
            }

        } catch (SQLException e) {
            // Rollback only the current failed batch
            connection.rollback();
            embedStep.markFailed("EMBED_ERROR", e.getMessage());
            result.finishFailed("EMBED_ERROR", e.getMessage());
            throw e;
        }
    }
}
