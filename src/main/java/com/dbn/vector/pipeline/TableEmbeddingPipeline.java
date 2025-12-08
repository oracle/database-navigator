package com.dbn.vector.pipeline;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.vector.model.PipelineStep;
import com.dbn.vector.model.StepResult;
import com.dbn.vector.model.TableResult;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.sourceconfig.DBTableSourceConfig;
import com.dbn.vector.service.TableProcessingService;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.vector.model.PipelineStep.ENSURE_DOCUMENT_TABLE;


public class TableEmbeddingPipeline extends EmbeddingPipeline {
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
        
        TableResult tableResult = result.initTableResult(
                tableConfig.getSchemaName(),
                tableConfig.getTableName()
        );

        String metadata =  tableProcessingService.buildRowMetadata(request,tableConfig);
        progressIndicator.setText2("Embedding table data from " + tableResult.getName());

        // Execute the embedding
        embedTableData(
                request,
                connection,
                vectorInterface,
                tableResult,
                metadata
        );
    }

    /**
     * Embed data from the source table.
     */
    private void embedTableData(
            @NotNull VectorEmbeddingRequest request,
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull TableResult tableResult,
            @NotNull String metadata) throws SQLException {

        StepResult embedStep = tableResult.startStep(PipelineStep.EMBED);

        try {
            int embeddedRows = vectorInterface.embedDataContent(
                    connection,
                    request.getSourceConfig().getTableSourceConfig(),
                    request.getChunkConfig().getConfigJson(),
                    request.getEmbedConfig().getConfigJson(),
                    request.getStoreConfig(),
                    metadata
            );

            embedStep.markSuccess();
            tableResult.finishSuccess(embeddedRows);

        } catch (SQLException e) {
            embedStep.markFailed("EMBED_ERROR", e.getMessage());
            tableResult.finishFailed("EMBED_ERROR", e.getMessage());
        }
    }
}
