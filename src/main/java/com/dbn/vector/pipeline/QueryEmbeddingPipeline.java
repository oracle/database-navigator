package com.dbn.vector.pipeline;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.vector.model.VectorEmbeddingContext;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.request.EmbeddingSourceQueries;
import com.dbn.vector.model.request.EmbeddingSourceQuery;
import com.dbn.vector.model.result.EmbeddingQueryResult;
import com.dbn.vector.model.result.PipelineStep;
import com.dbn.vector.model.result.StepResult;
import com.dbn.vector.service.QueryProcessingService;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import static com.dbn.connection.Resources.commit;
import static com.dbn.connection.Resources.rollbackSilently;


public class QueryEmbeddingPipeline extends EmbeddingPipeline {
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final QueryProcessingService queryProcessingService = new QueryProcessingService();

    @Override
    protected void executeSourceSpecificPipeline(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull VectorEmbeddingResult result) {

        EmbeddingSourceQueries sources = request.getSourceConfig().getSourceQueries();
        for (EmbeddingSourceQuery source : sources.getElements()) {
            EmbeddingQueryResult queryResult = result.getResult(source);

            String metadata = queryProcessingService.buildRowMetadata(request, source);
            queryResult.setMetadata(metadata);
            context.getProgressIndicator().setText2("Processing query " + queryResult.getName());

            // Execute the embedding with batching
            embedQueryDataInBatches(context, request, queryResult);
        }

    }

    /**
     * Embed data from the source table using batching for failure recovery.
     * Each batch is committed separately, so progress is preserved on failure.
     */
    private void embedQueryDataInBatches(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull EmbeddingQueryResult result) {

        StepResult embedStep = result.startStep(PipelineStep.EMBED);
        DBNConnection connection = context.getConnection();
        ProgressIndicator progressIndicator = context.getProgressIndicator();

        try {
            int totalRowsEmbedded = 0;
            int batchNumber = 0;

            connection.setAutoCommit(false);
            while (true) {
                if (progressIndicator.isCanceled()) break;

                batchNumber++;
                progressIndicator.setText2("Processing query " + result.getName() + " (batch " + batchNumber + " / rows embedded " + totalRowsEmbedded + ")");

                // Process one batch
                DatabaseVectorInterface vectorInterface = request.getConnection().getVectorInterface();
                int rowsEmbedded = vectorInterface.embedQueryContent(
                        connection,
                        result.getSelectStatement(),
                        request.getChunkConfig().getConfigJson(),
                        request.getModelConfig().getConfigJson(),
                        request.getDestinationConfig(),
                        result.getMetadata(),
                        DEFAULT_BATCH_SIZE);

                // Commit after each batch - this is the recovery point
                commit(connection);

                totalRowsEmbedded += rowsEmbedded;
                if (rowsEmbedded == 0) break;
            }

            if (progressIndicator.isCanceled()) {
                embedStep.markSuccess();
                result.finishSuccess(totalRowsEmbedded);
            } else {
                embedStep.markSuccess();
                result.finishSuccess(totalRowsEmbedded);
            }

        } catch (Exception e) {
            // Rollback only the current failed batch
            rollbackSilently(connection);
            embedStep.markFailed("EMBED_ERROR", e.getMessage());
            result.finishFailed("EMBED_ERROR", e.getMessage());
        }
    }
}
