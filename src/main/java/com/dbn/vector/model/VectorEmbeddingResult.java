package com.dbn.vector.model;

import com.dbn.common.message.MessageType;
import com.dbn.common.message.TitledMessage;
import com.dbn.common.task.TaskStatus;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.util.UUIDs;
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
import static com.dbn.nls.NlsResources.txt;

@Getter
@Setter
public class VectorEmbeddingResult {
    private final String id = UUIDs.compact();
    private final VectorEmbeddingRequest request;
    private final Map<String, EmbeddingResult> results = new LinkedHashMap<>();
    private final Listeners<Runnable> listeners = Listeners.create();

    public EmbeddingSourceType getSourceType() {
        return request.getSourceConfig().getSourceType();
    }

    public enum Status {RUNNING, SUCCESS, PARTIAL, FAILED, CANCELLED}

    private Status status = Status.RUNNING;
    private volatile boolean cancellationRequested;

    public VectorEmbeddingResult(VectorEmbeddingRequest request) {
        this.request = request;
        initResultElements();
    }

    private void initResultElements() {
        EmbeddingSourceConfig sourceConfig = request.getSourceConfig();

        switch (sourceConfig.getSourceType()) {
            case FILE_SYSTEM -> sourceConfig.getSourceFiles().forEach(s -> addResult(s.getIdentifier(), new EmbeddingFileResult(s)));
            case DATABASE_TABLE -> sourceConfig.getSourceTables().forEach(s -> addResult(s.getIdentifier(), new EmbeddingTableResult(s, getConnectionId())));
            case DATABASE_QUERY -> sourceConfig.getSourceQueries().forEach(s -> addResult(s.getIdentifier(), new EmbeddingQueryResult(s)));
        }
    }

    private void addResult(String identifier, EmbeddingResult result) {
        result.setChangeListener(this::notifyChanged);
        results.put(identifier, result);
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
        status = evaluateStatus();
        notifyChanged();
    }

    private Status evaluateStatus() {
        if (cancellationRequested) return Status.CANCELLED;

        boolean anySuccess = results.values().stream().anyMatch(f -> f.getStatus() == TaskStatus.DONE);
        boolean anyFailed = results.values().stream().anyMatch(f -> f.getStatus() == TaskStatus.FAILED);
        boolean anySkipped = results.values().stream().anyMatch(f -> f.getStatus() == TaskStatus.SKIPPED);

        if (anySuccess && anyFailed) return Status.PARTIAL;
        if (anySuccess) return Status.SUCCESS;
        if (anyFailed) return Status.FAILED;
        if (anySkipped) return Status.SUCCESS; // All skipped means already processed = success
        return Status.SUCCESS;
    }

    public void cancel() {
        cancellationRequested = true;
        notifyChanged();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void notifyChanged() {
        listeners.notify(Runnable::run);
    }

    public long getSourceSucceedCount() {
        return results.values().stream().filter(f -> f.getStatus() == TaskStatus.DONE).count();
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
        long successCount = results.values().stream()
                .filter(f -> f.getStatus() == TaskStatus.DONE || f.getStatus() == TaskStatus.SKIPPED)
                .count();
        if (getResourcesCount() == 0) return 0;

        double successRate = (double) successCount / getResourcesCount() * 100;
        return Math.round(successRate * 100) / 100.0;
    }

    public TitledMessage getSummaryMessage() {
        MessageType messageType = switch (status) {
            case RUNNING -> MessageType.PROCESSING;
            case CANCELLED -> MessageType.WARNING;
            case SUCCESS -> MessageType.SUCCESS;
            case PARTIAL -> MessageType.WARNING;
            case FAILED -> MessageType.ERROR;
            default -> MessageType.INFO;
        };

        String title = switch (status) {
            case RUNNING -> txt("prc.vector.title.EmbeddingData");
            case CANCELLED -> txt("msg.vector.title.EmbeddingCancelled");
            case SUCCESS -> txt("msg.vector.title.EmbeddingSuccessful");
            case PARTIAL -> txt("msg.vector.title.EmbeddingPartiallySuccessful");
            case FAILED -> txt("msg.vector.title.EmbeddingFailed");
            default -> "";
        };

        String message = switch (status) {
            case RUNNING -> txt("prc.vector.text.EmbeddingData",
                    request.getDestinationConfig().getSchemaName(),
                    request.getDestinationConfig().getTableName());
            case CANCELLED -> txt("msg.vector.text.EmbeddingCancelled",
                    getSourceSucceedCount(), getResourcesCount());
            case SUCCESS -> txt("msg.vector.text.EmbeddingSuccessful", getResourcesCount());
            case PARTIAL -> txt("msg.vector.text.EmbeddingPartiallySuccessful", getSourceSucceedCount(), getResourcesCount());
            case FAILED -> txt("msg.vector.text.EmbeddingFailed");
            default -> "";
        };

        return new TitledMessage(messageType, title, message);
    }
}
