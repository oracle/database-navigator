package com.dbn.vector.model;

import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.task.TaskStatus;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.vector.model.request.EmbeddingSource;
import com.dbn.vector.model.request.EmbeddingSourceConfig;
import com.dbn.vector.model.request.EmbeddingSourceType;
import com.dbn.vector.model.result.EmbeddingFileResult;
import com.dbn.vector.model.result.EmbeddingQueryResult;
import com.dbn.vector.model.result.EmbeddingResult;
import com.dbn.vector.model.result.EmbeddingTableResult;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public class VectorEmbeddingResult {
    private final VectorEmbeddingRequest request;
    private final Map<String, EmbeddingResult> results = new LinkedHashMap<>();

    public EmbeddingSourceType getSourceType() {
        return request.getSourceConfig().getSourceType();
    }

    public enum Status {RUNNING, SUCCESS, PARTIAL, FAILED}

    private Status status;

    public VectorEmbeddingResult(VectorEmbeddingRequest request) {
        this.request = request;
        initResultElements();
    }

    private void initResultElements() {
        EmbeddingSourceConfig sourceConfig = request.getSourceConfig();

        switch (sourceConfig.getSourceType()) {
            case FILE_SYSTEM -> sourceConfig.getSourceFiles().forEach(s -> results.put(s.getIdentifier(), new EmbeddingFileResult(s)));
            case DATABASE_TABLE -> sourceConfig.getSourceTables().forEach(s -> results.put(s.getIdentifier(), new EmbeddingTableResult(s, getConnectionId())));
            case DATABASE_QUERY -> sourceConfig.getSourceQueries().forEach(s -> results.put(s.getIdentifier(), new EmbeddingQueryResult(s)));
        }
    }

    public <R extends EmbeddingResult> R getResult(EmbeddingSource source) {
        return cast(results.get(source.getIdentifier()));
    }

    public ConnectionHandler getConnection() {
        return request.getConnection();
    }

    public ConnectionId getConnectionId() {
        return request.getConnectionId();
    }

    public int size() {
        return results.size();
    }



    /**
     * Mark the job finished and compute aggregated status.
     */
    public void finish() {

        boolean anySuccess = results.values().stream().anyMatch(f -> f.getStatus() == TaskStatus.FINISHED);
        boolean anyFailed = results.values().stream().anyMatch(f -> f.getStatus() == TaskStatus.FAILED);
        boolean anySkipped = results.values().stream().anyMatch(f -> f.getStatus() == TaskStatus.SKIPPED);

        if (anySuccess && anyFailed) status = Status.PARTIAL;
        else if (anySuccess) status = Status.SUCCESS;
        else if (anyFailed) status = Status.FAILED;
        else if (anySkipped) status = Status.SUCCESS; // All skipped means already processed = success
        else status = Status.SUCCESS;
    }

    public long getSourceSucceedCount() {
        return results.values().stream().filter(f -> f.getStatus() == TaskStatus.FINISHED).count();
    }

    public long getDuration() {
        return results.values().stream().mapToLong(r -> r.getDuration()).sum();
    }

    public long getTotalInsertedRows() {
        return results.values().stream().mapToLong(EmbeddingResult::getRowsInserted).sum();
    }

    public List<EmbeddingResult> getResults() {
        return new ArrayList<>(results.values());
    }

    public long getResourcesCount() {
        return results.size();
    }

    public double getSuccessRate() {
        long successedSr = results.values().stream()
                .filter(f -> f.getStatus() == TaskStatus.FINISHED || f.getStatus() == TaskStatus.SKIPPED)
                .count();
        return getResourcesCount() > 0 ? (double) successedSr / getResourcesCount() * 100 : 0;
    }

    public TitledMessage getSummaryMessage() {
        MessageType messageType = switch (status) {
            case SUCCESS -> MessageType.SUCCESS;
            case PARTIAL -> MessageType.WARNING;
            case FAILED -> MessageType.ERROR;
            default -> MessageType.INFO;
        };

        String title = switch (status) {
            case SUCCESS -> "Embedding successful";
            case PARTIAL -> "Embedding partially successful";
            case FAILED -> "Embedding failed";
            default -> "";
        };

        String message = switch (status) {
            case SUCCESS -> "Successfully embedded contents from " + getResourcesCount() + " sources";
            case PARTIAL -> "Embedded contents from " + getSourceSucceedCount() + " out of " + getResourcesCount() + " sources";
            case FAILED -> "Embedding failed for all given sources";
            default -> "";
        };

        return new TitledMessage(messageType, title, message);
    }
}

