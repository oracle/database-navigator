package com.dbn.vector.pipeline;

import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.vector.model.PipelineStep;
import com.dbn.vector.model.StepResult;
import com.dbn.vector.model.VectorEmbeddingRequest;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.store.DestinationType;
import com.dbn.vector.model.store.StoreConfig;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.type.DBObjectType.TABLE;


public abstract class EmbeddingPipeline {

    protected static final String FILES_TABLE = "document_files";

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
        ensureDestinationTableStep(request, connection, vectorInterface,ensureDestStep);

        if (ensureDestStep.getStatus() == StepResult.STEP_STATUS.FAILED && ensureDestStep.isCritical()) {
            return;
        }

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
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface, StepResult step) {

        step.start();

        try {
            StoreConfig storeConfig = request.getStoreConfig();
            step.setLink(storeConfig.getSchemaName()+"."+storeConfig.getTableName());
            step.setIcon(Icons.DBO_TABLE);
            if (storeConfig.getDestinationType() == DestinationType.NEW_TABLE) {
                vectorInterface.createEmbeddingTable(
                        connection,
                        storeConfig.getSchemaName(),
                        storeConfig.getTableName(),
                        storeConfig.getKeyColumnName(),
                        storeConfig.getTextColumnName(),
                        storeConfig.getEmbeddingColumnName(),
                        storeConfig.getMetadataColumnName()
                );


                // Notify browser to refresh
                notifyTableCreated(request.getConnectionId(), storeConfig.getSchemaName());
            }

            step.markSuccess();

        } catch (SQLException e) {
            step.markFailed("ENSURE_DEST_ERROR", e.getMessage());
        }

        return step;
    }

    /**
     * Ensure the documents metadata table exists (for file sources).
     * This is also a shared step for all file-based sources.
     */
    protected StepResult ensureDocumentsTableStep(
            @NotNull DBNConnection connection,
            @NotNull DatabaseVectorInterface vectorInterface,
            StepResult step,
            String schemaName) {

        step.start();
        boolean tableWasCreated = false;
        String tableIdentifier = schemaName + "." + FILES_TABLE.toUpperCase();

        try {
            vectorInterface.ensureDocumentsTable(connection, schemaName, FILES_TABLE);
            step.markSuccess();
            step.setLink(tableIdentifier);
            step.setIcon(Icons.DBO_TABLE);

        } catch (SQLException e) {
            int errorCode = e.getErrorCode();

            if (errorCode == 20001) {
                // success
                tableWasCreated = true;
                step.markSuccess();

                step.setLink(tableIdentifier);
                step.setIcon(Icons.DBO_TABLE);

            } else if (errorCode == 20002) {
                //success
                tableWasCreated = false;
                step.markSuccess();
                step.setLink(tableIdentifier);
                step.setIcon(Icons.DBO_TABLE);

            } else {
                // Failed
                step.markFailed("DOCUMENTS_TABLE_ERROR", e.getMessage());
                return step;
            }
        }

        if (tableWasCreated) {
            notifyTableCreated(connection.getConnectionId(),schemaName);
        }


            return step;
    }

    private void notifyTableCreated(ConnectionId connectionId, String schemaName) {
        SchemaId schemaId = SchemaId.get(schemaName);
        ObjectChangeEvent.notify(
                CREATE,
                TABLE,
                connectionId,
                schemaId
        );
    }

    /**
     * Notify the object browser that a new table was created.
     */
//    private void notifyTableCreated(@NotNull VectorEmbeddingRequest request, @NotNull StoreConfig storeConfig) {
//        SchemaId schemaId = SchemaId.get(storeConfig.getSchemaName());
//        ObjectChangeEvent.notify(
//                CREATE,
//                TABLE,
//                request.getConnectionId(),
//                schemaId
//        );
//    }
}
