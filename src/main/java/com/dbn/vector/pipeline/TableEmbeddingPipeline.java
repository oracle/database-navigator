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
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;


public class TableEmbeddingPipeline extends EmbeddingPipeline {

    @Override
    protected void executeSourceSpecificPipeline(
            @NotNull VectorEmbeddingRequest request,
            @NotNull ConnectionHandler handler,
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull ProgressIndicator progressIndicator,
            @NotNull VectorEmbeddingResult result) throws Exception {

        DBTableSourceConfig tableConfig = request.getSourceConfig().getTableSourceConfig();
        
        TableResult tableResult = result.initTableResult(
                tableConfig.getSchemaName(),
                tableConfig.getTableName()
        );

        progressIndicator.setText2("Embedding table data from " + tableResult.getName());

        // Execute the embedding
        embedTableData(
                request,
                connection,
                vectorInterface,
                tableResult
        );
    }

    /**
     * Embed data from the source table.
     */
    private void embedTableData(
            @NotNull VectorEmbeddingRequest request,
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            @NotNull TableResult tableResult) {

        StepResult embedStep = tableResult.startStep(PipelineStep.EMBED);

        try {
            int embeddedRows = vectorInterface.embedDataContent(
                    connection,
                    request.getSourceConfig().getTableSourceConfig(),
                    request.getChunkConfig().getConfigJson(),
                    request.getEmbedConfig().getConfigJson(),
                    request.getStoreConfig()
            );

            embedStep.markSuccess();
            tableResult.finishSuccess(embeddedRows);

        } catch (SQLException e) {
            embedStep.markFailed("EMBED_ERROR", e.getMessage());
            tableResult.finishFailed("EMBED_ERROR", e.getMessage());
        }
    }
}
