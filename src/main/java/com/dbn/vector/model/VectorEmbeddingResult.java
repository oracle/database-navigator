package com.dbn.vector.model;

import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.sourceconfig.SourceType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
@Getter
@Setter
public class VectorEmbeddingResult {
  ConnectionHandler connectionHandler;


  private enum Status { RUNNING, SUCCESS, PARTIAL, FAILED }
  private Status status;
  private SourceType sourceType;
  private List<SourceResult> sourceResults = new ArrayList<>();

  public VectorEmbeddingResult(@Nullable ConnectionHandler connectionHandler) {
    this.connectionHandler = connectionHandler;
  }
  /** Mark the job finished and compute aggregated status. */
  public void finish() {
    boolean anySuccess = sourceResults.stream().anyMatch(f -> f.getStatus() == SourceStatus.SUCCESS);
    boolean anyFailed = sourceResults.stream().anyMatch(f -> f.getStatus() == SourceStatus.FAILED);
    if (anySuccess && anyFailed) status = Status.PARTIAL;
    else if (anySuccess) status = Status.SUCCESS;
    else if (anyFailed) status = Status.FAILED;
    else status = Status.SUCCESS;
  }
}

