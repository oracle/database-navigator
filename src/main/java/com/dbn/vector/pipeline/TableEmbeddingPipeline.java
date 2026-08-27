package com.dbn.vector.pipeline;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.vector.model.VectorEmbeddingContext;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.request.EmbeddingDestinationConfig;
import com.dbn.vector.model.request.EmbeddingSourceTable;
import com.dbn.vector.model.request.EmbeddingSourceTables;
import com.dbn.vector.model.result.EmbeddingTableResult;
import com.dbn.vector.model.result.PipelineStep;
import com.dbn.vector.model.result.StepResult;
import com.dbn.vector.service.TableProcessingService;
import org.jetbrains.annotations.NotNull;

import static com.dbn.connection.Resources.commit;
import static com.dbn.connection.Resources.rollbackSilently;


public class TableEmbeddingPipeline implements EmbeddingPipeline {
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final TableProcessingService tableProcessingService = new TableProcessingService();

    @Override
    public void execute(@NotNull VectorEmbeddingContext context) {
        VectorEmbeddingRequest request = context.getRequest();
        VectorEmbeddingResult result = context.getResult();

        EmbeddingSourceTables tableConfig = request.getSourceConfig().getSourceTables();
        for (EmbeddingSourceTable tableSource : tableConfig.getElements()) {
            if (context.isCancellationRequested()) break;
            EmbeddingTableResult tableResult = result.getResult(tableSource);

            String metadata = tableProcessingService.buildRowMetadata(request, tableSource);
            tableResult.setMetadata(metadata);
            // Execute the embedding with batching
            embedTableDataInBatches(context, request, tableResult);
        }

    }

    /**
     * Embed data from the source table using batching for failure recovery.
     * Each batch is committed separately, so progress is preserved on failure.
     */
    private void embedTableDataInBatches(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull EmbeddingTableResult result) {

        StepResult embedStep = result.startStep(PipelineStep.EMBED);
        DBNConnection connection = context.getConnection();

        try {
            int totalRowsEmbedded = 0;
            int batchNumber = 0;

            connection.setAutoCommit(false);
            while (true) {
                if (context.isCancellationRequested()) break;

                batchNumber++;
                // Process one batch
                DatabaseVectorInterface vectorInterface = request.getConnection().getVectorInterface();
                EmbeddingDestinationConfig destinationConfig = request.getDestinationConfig();
                int rowsEmbedded = vectorInterface.embedTableContent(
                        connection,
                        result.getSource(),
                        request.getChunkConfigJson(),
                        request.getModelConfigJson(),
                        destinationConfig.getSchemaName(),
                        destinationConfig.getTableName(),
                        result.getMetadata(), DEFAULT_BATCH_SIZE);

                // Commit after each batch - this is the recovery point
                commit(connection);

                totalRowsEmbedded += rowsEmbedded;
                if (rowsEmbedded == 0) break;
            }

            if (context.isCancellationRequested()) {
                embedStep.markSuccess();
                result.finishSuccess(totalRowsEmbedded);
            } else {
                embedStep.markSuccess();
                result.finishSuccess(totalRowsEmbedded);
            }

        } catch (Exception e) {
            // Rollback only the current failed batch
            rollbackSilently(connection);
            embedStep.markFailed("EMBED_ERROR", e);
            result.finishFailed("EMBED_ERROR", e);
        }
    }
}
