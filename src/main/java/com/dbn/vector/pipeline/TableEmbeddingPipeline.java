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
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import static com.dbn.connection.Resources.commit;
import static com.dbn.connection.Resources.rollbackSilently;
import static com.dbn.nls.NlsResources.txt;


public class TableEmbeddingPipeline implements EmbeddingPipeline {
    private static final int DEFAULT_BATCH_SIZE = 100;

    private final TableProcessingService tableProcessingService = new TableProcessingService();

    @Override
    public void execute(
            @NotNull VectorEmbeddingContext context,
            @NotNull VectorEmbeddingRequest request,
            @NotNull VectorEmbeddingResult result) {

        EmbeddingSourceTables tableConfig = request.getSourceConfig().getSourceTables();
        for (EmbeddingSourceTable tableSource : tableConfig.getElements()) {
            EmbeddingTableResult tableResult = result.getResult(tableSource);

            String metadata = tableProcessingService.buildRowMetadata(request, tableSource);
            tableResult.setMetadata(metadata);
            context.getProgressIndicator().setText2(txt("prc.vector.text.ProcessingTable", tableResult.getName()));

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
        ProgressIndicator progressIndicator = context.getProgressIndicator();

        try {
            int totalRowsEmbedded = 0;
            int batchNumber = 0;

            connection.setAutoCommit(false);
            while (true) {
                if (progressIndicator.isCanceled()) break;

                batchNumber++;
                progressIndicator.setText2(txt("prc.vector.text.ProcessingTableBatch", result.getName(), batchNumber, totalRowsEmbedded));

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
            embedStep.markFailed("EMBED_ERROR", e);
            result.finishFailed("EMBED_ERROR", e);
        }
    }
}
