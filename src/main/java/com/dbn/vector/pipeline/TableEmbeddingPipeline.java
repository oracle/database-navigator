package com.dbn.vector.pipeline;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseAssistantInterface;
import com.dbn.vector.model.*;
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
            @NotNull DatabaseAssistantInterface assistantInterface,
            @NotNull ProgressIndicator progressIndicator,
            @NotNull VectorEmbeddingResult result,
            @NotNull StepResult ensureDestStep) throws Exception {

        DBTableSourceConfig tableConfig = request.getSourceConfig().getTableSourceConfig();
        
        TableResult tableResult = result.ensureSourceResult(
                tableConfig.getSchemaName(),
                tableConfig.getTableName()
        );

        // Add the shared destination step to this table result
        tableResult.getSteps().add(ensureDestStep);

        progressIndicator.setText2("Embedding table data from " + tableResult.getName());

        // Execute the embedding
        embedTableData(
                request,
                connection,
                assistantInterface,
                tableResult
        );
    }

    /**
     * Embed data from the source table.
     */
    private void embedTableData(
            @NotNull VectorEmbeddingRequest request,
            @NotNull DBNConnection connection,
            @NotNull DatabaseAssistantInterface assistantInterface,
            @NotNull TableResult tableResult) {

        StepResult embedStep = tableResult.startStep(PipelineStep.EMBED);

        try {
            String chunkConfigJson = getConfigJson(request.getChunkConfig());
            String embedConfigJson = getConfigJson(request.getEmbedConfig());

            int embeddedRows = assistantInterface.embedDataContent(
                    connection,
                    request.getSourceConfig().getTableSourceConfig(),
                    chunkConfigJson,
                    embedConfigJson,
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
