package com.dbn.vector.model;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.vector.model.request.EmbeddingFileSource;
import com.dbn.vector.model.request.EmbeddingFileSources;
import com.dbn.vector.model.request.EmbeddingSource;
import com.dbn.vector.model.request.EmbeddingSourceConfig;
import com.dbn.vector.model.request.EmbeddingTableSource;
import com.dbn.vector.model.request.EmbeddingTableSources;
import com.dbn.vector.model.result.EmbeddingFileResult;
import com.dbn.vector.model.result.EmbeddingResult;
import com.dbn.vector.model.result.EmbeddingTableResult;
import com.dbn.vector.model.result.PipelineStep;
import com.dbn.vector.model.result.SourceStatus;
import com.dbn.vector.model.result.StepResult;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Unsafe.cast;

@Getter
@Setter
public class VectorEmbeddingResult {
    private final VectorEmbeddingRequest request;
    private final Map<String, EmbeddingResult> results = new LinkedHashMap<>();

    public enum Status {RUNNING, SUCCESS, PARTIAL, FAILED}

    private Status status;
    protected final List<StepResult> sharedSteps = new ArrayList<>(Arrays.asList(
            new StepResult(PipelineStep.ENSURE_DESTINATION),
            new StepResult(PipelineStep.ENSURE_DOCUMENT_TABLE)
    ));


    public StepResult getstep(PipelineStep step) {
        for (StepResult stepResult : sharedSteps) {
            if (stepResult.getStep().equals(step)) {
                return stepResult;
            }
        }
        return null;
    }

    public VectorEmbeddingResult(VectorEmbeddingRequest request) {
        this.request = request;
        initResultElements();
    }

    private void initResultElements() {
        EmbeddingSourceConfig sourceConfig = request.getSourceConfig();

        switch (sourceConfig.getSourceType()) {
            case FILE_SYSTEM:
                EmbeddingFileSources fileConfig = sourceConfig.getSourceFiles();
                for (EmbeddingFileSource source : fileConfig.getElements()) {
                    results.put(source.getIdentifier(), new EmbeddingFileResult(source));
                }
                break;
            case DATABASE_TABLE:
                EmbeddingTableSources tableConfig = sourceConfig.getSourceTables();
                for (EmbeddingTableSource source : tableConfig.getElements()) {
                    results.put(source.getIdentifier(), new EmbeddingTableResult(source, getConnectionId()));
                }
                break;
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

        boolean anySuccess = results.values().stream().anyMatch(f -> f.getStatus() == SourceStatus.SUCCESS);
        boolean anyFailed = results.values().stream().anyMatch(f -> f.getStatus() == SourceStatus.FAILED);
        boolean anySkipped = results.values().stream().anyMatch(f -> f.getStatus() == SourceStatus.SKIPPED);

        if (anySuccess && anyFailed) status = Status.PARTIAL;
        else if (anySuccess) status = Status.SUCCESS;
        else if (anyFailed) status = Status.FAILED;
        else if (anySkipped) status = Status.SUCCESS; // All skipped means already processed = success
        else status = Status.SUCCESS;
    }

    public long getSourceSucceedCount() {
        return results.values().stream().filter(f -> f.getStatus() == SourceStatus.SUCCESS).count();
    }

    public long getDuration() {
        return results.values().stream().mapToLong(EmbeddingResult::getDurationMs).sum();
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
                .filter(f -> f.getStatus() == SourceStatus.SUCCESS || f.getStatus() == SourceStatus.SKIPPED)
                .count();
        return getResourcesCount() > 0 ? (double) successedSr / getResourcesCount() * 100 : 0;
    }


    public void deleteStepFfromShared(PipelineStep pipelineStep) {
        sharedSteps.removeIf((step) -> step.getStep().equals(pipelineStep));
    }
}

