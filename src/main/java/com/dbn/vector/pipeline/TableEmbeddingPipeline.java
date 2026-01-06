package com.dbn.vector.pipeline;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.vector.model.PipelineStep;
import com.dbn.vector.model.StepResult;
import com.dbn.vector.model.TableResult;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.source.DBTableSourceConfig;
import com.dbn.vector.model.sourceconfig.DbTableSource;
import com.dbn.vector.service.TableProcessingService;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.vector.model.PipelineStep.ENSURE_DOCUMENT_TABLE;


public class TableEmbeddingPipeline extends EmbeddingPipeline {
    private static final int DEFAULT_BATCH_SIZE = 100;
    
    private final TableProcessingService tableProcessingService = new TableProcessingService();
    
    @Override
    protected void executeSourceSpecificPipeline(
            @NotNull VectorEmbeddingRequest request,
            @NotNull ConnectionHandler handler,
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull ProgressIndicator progressIndicator,
            @NotNull VectorEmbeddingResult result) throws Exception {

        // remove the PREPARE_DOCUMENT_STORE step from shared steps
        result.deleteStepFfromShared(ENSURE_DOCUMENT_TABLE);
        DBTableSourceConfig tableConfig = request.getSourceConfig().getTableSourceConfig();


        for (DbTableSource tableSource : tableConfig.getDbTableSources()){
          TableResult tableResult = result.initTableResult(
                  tableSource.getSchemaName(),
                  tableSource.getTableName()
          );

          String metadata = tableProcessingService.buildRowMetadata(request, tableSource);
          progressIndicator.setText2("Embedding table data from " + tableResult.getName());

          // Execute the embedding with batching
          embedTableDataInBatches(
                  request,
                  connection,
                  vectorInterface,
                  tableResult,
                  tableSource,
                  metadata,
                  progressIndicator
          );
        }

    }

    /**
     * Embed data from the source table using batching for failure recovery.
     * Each batch is committed separately, so progress is preserved on failure.
     */
    private void embedTableDataInBatches(
            @NotNull VectorEmbeddingRequest request,
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull TableResult tableResult,
            DbTableSource tableSource, @NotNull String metadata,
            @NotNull ProgressIndicator progressIndicator) throws SQLException {

        StepResult embedStep = tableResult.startStep(PipelineStep.EMBED);

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
                progressIndicator.setText2("Processing batch " + batchNumber + " (total rows: " + totalProcessed + ")");

                // Process one batch
                batchCount = vectorInterface.embedDataContent(
                        connection,
                        tableSource,
                        request.getChunkConfig().getConfigJson(),
                        request.getEmbedConfig().getConfigJson(),
                        request.getStoreConfig(),
                        metadata,
                        DEFAULT_BATCH_SIZE
                );

                // Commit after each batch - this is the recovery point
                connection.commit();

                totalProcessed += batchCount;

            } while (batchCount > 0);

            if (progressIndicator.isCanceled()) {
                embedStep.markSuccess();
                tableResult.finishSuccess(totalProcessed);
            } else {
                embedStep.markSuccess();
                tableResult.finishSuccess(totalProcessed);
            }

        } catch (SQLException e) {
            // Rollback only the current failed batch
            connection.rollback();
            embedStep.markFailed("EMBED_ERROR", e.getMessage());
            tableResult.finishFailed("EMBED_ERROR", e.getMessage());
            throw e;
        }
    }
}
