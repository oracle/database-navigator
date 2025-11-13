package com.dbn.vector.model;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.vector.model.sourceconfig.SourceType;
import com.intellij.openapi.vfs.VirtualFile;
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

    public ConnectionHandler getConnection() {
        return request.getConnection();
    }

  private enum Status { RUNNING, SUCCESS, PARTIAL, FAILED }
  private Status status;
  private SourceType sourceType;
  private Map<String, SourceResult> sourceResults = new LinkedHashMap<>();

  public VectorEmbeddingResult(VectorEmbeddingRequest request) {
    this.request = request;
  }

  public int size() {
    return sourceResults.size();
  }

  /** Mark the job finished and compute aggregated status. */
  public void finish() {
    boolean anySuccess = sourceResults.values().stream().anyMatch(f -> f.getStatus() == SourceStatus.SUCCESS);
    boolean anyFailed = sourceResults.values().stream().anyMatch(f -> f.getStatus() == SourceStatus.FAILED);
    if (anySuccess && anyFailed) status = Status.PARTIAL;
    else if (anySuccess) status = Status.SUCCESS;
    else if (anyFailed) status = Status.FAILED;
    else status = Status.SUCCESS;
  }

  public TableResult ensureSourceResult(String schemaName, String tableName) {
    ConnectionId connectionId = getConnection().getConnectionId();
    String key = schemaName + "." + tableName;
    return cast(sourceResults.computeIfAbsent(key, k ->
            new TableResult(connectionId, schemaName, tableName)));
  }

  public FileResult ensureFileResult(VirtualFile file) {
    String key = file.getPath();
    return cast(sourceResults.computeIfAbsent(key, k -> new FileResult(file)));
  }

  public List<SourceResult> getSourceResults() {
    return new ArrayList<>(sourceResults.values());
  }

  @Deprecated // TODO use lazy result initialization utilities (support multiple table sources)
  public void addSourceResult(SourceResult sourceResult) {
    sourceResults.put(sourceResult.getIdentifier(), sourceResult);
  }

}

