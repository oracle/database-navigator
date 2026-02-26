package com.dbn.vector.pipeline;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.vector.model.VectorEmbeddingContext;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.request.EmbeddingSourceTable;
import com.dbn.vector.model.request.EmbeddingSourceTables;
import com.dbn.vector.model.result.EmbeddingTableResult;
import com.dbn.vector.model.result.PipelineStep;
import com.dbn.vector.model.result.StepResult;
import com.dbn.vector.service.TableProcessingService;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.vector.model.result.PipelineStep.ENSURE_DOCUMENT_TABLE;


public class TableEmbeddingPipeline extends EmbeddingPipeline {
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final TableProcessingService tableProcessingService = new TableProcessingService();

    @Override
    protected void executeSourceSpecificPipeline(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull VectorEmbeddingResult result) throws Exception {

        // remove the PREPARE_DOCUMENT_STORE step from shared steps
        result.deleteStepFfromShared(ENSURE_DOCUMENT_TABLE);
        EmbeddingSourceTables tableConfig = request.getSourceConfig().getSourceTables();

        for (EmbeddingSourceTable tableSource : tableConfig.getElements()) {
            EmbeddingTableResult tableResult = result.getResult(tableSource);

            String metadata = tableProcessingService.buildRowMetadata(request, tableSource);
            tableResult.setMetadata(metadata);
            context.getProgressIndicator().setText2("Processing table " + tableResult.getName());

            // Execute the embedding with batching
            embedTableDataInBatches(
                    context, request,
                    tableResult
            );
        }

    }

    /**
     * Embed data from the source table using batching for failure recovery.
     * Each batch is committed separately, so progress is preserved on failure.
     */
    private void embedTableDataInBatches(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull EmbeddingTableResult result) throws SQLException {

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
                progressIndicator.setText2("Processing table " + result.getName() + " (batch " + batchNumber + " / rows embedded " + totalProcessed + ")");

                // Process one batch
                DatabaseVectorInterface vectorInterface = request.getConnection().getVectorInterface();
                batchCount = vectorInterface.embedTableContent(
                        connection,
                        result.getSource(),
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
