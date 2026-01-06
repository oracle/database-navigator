package com.dbn.vector.model;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.vector.model.source.DBTableSourceConfig;
import com.dbn.vector.model.source.FileSystemSourceConfig;
import com.dbn.vector.model.source.SourceConfig;
import com.dbn.vector.model.source.SourceType;
import com.dbn.vector.model.sourceconfig.DbTableSource;
import com.intellij.openapi.vfs.VirtualFile;
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

    public ConnectionHandler getConnection() {
        return request.getConnection();
    }




  public  enum Status { RUNNING, SUCCESS, PARTIAL, FAILED }
  private Status status;
  private SourceType sourceType;
  private Map<String, SourceResult> sourceResults = new LinkedHashMap<>();
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
    initSourceResults();
  }

  private void initSourceResults() {
    SourceConfig sourceConfig = request.getSourceConfig();

    switch (sourceConfig.getSourceType()) {
      case FILE_SYSTEM:
        FileSystemSourceConfig fileConfig = sourceConfig.getFileSourceConfig();
        List<VirtualFile> files = fileConfig.getFiles();
        for (int i = 0; i < files.size(); i++) {
          initFileResult(files.get(i));
        }
        break;
      case DATABASE_TABLE:
        DBTableSourceConfig tableConfig = sourceConfig.getTableSourceConfig();
        for (DbTableSource tableSource : tableConfig.getDbTableSources()){
          initTableResult(tableSource.getSchemaName(), tableSource.getTableName());
        }
        break;
    }
  }

  public int size() {
    return sourceResults.size();
  }

  /** Mark the job finished and compute aggregated status. */
  public void finish() {

    boolean anySuccess = sourceResults.values().stream().anyMatch(f -> f.getStatus() == SourceStatus.SUCCESS);
    boolean anyFailed = sourceResults.values().stream().anyMatch(f -> f.getStatus() == SourceStatus.FAILED);
    boolean anySkipped = sourceResults.values().stream().anyMatch(f -> f.getStatus() == SourceStatus.SKIPPED);
    
    if (anySuccess && anyFailed) status = Status.PARTIAL;
    else if (anySuccess) status = Status.SUCCESS;
    else if (anyFailed) status = Status.FAILED;
    else if (anySkipped) status = Status.SUCCESS; // All skipped means already processed = success
    else status = Status.SUCCESS;
  }

  public long getSourceSucceedCount(){
    return sourceResults.values().stream().filter(f -> f.getStatus() == SourceStatus.SUCCESS).count();
  }
  public long  getDuration(){
    return sourceResults.values().stream().mapToLong(SourceResult::getDurationMs).sum();
  }

  public long getTotalInsertedRows(){
    return sourceResults.values().stream().mapToLong(SourceResult::getRowsInserted).sum();
  }

  public TableResult initTableResult(String schemaName, String tableName) {
    ConnectionId connectionId = getConnection().getConnectionId();
    String key = schemaName + "." + tableName;
    return cast(sourceResults.computeIfAbsent(key, k ->
            createTableResult(schemaName, tableName, connectionId)));
  }

  public FileResult initFileResult(VirtualFile file) {
    String key = file.getPath();
    return cast(sourceResults.computeIfAbsent(key, k -> createFileResult(file)));
  }

  private TableResult createTableResult(String schemaName, String tableName, ConnectionId connectionId) {
    TableResult tableResult = new TableResult(connectionId, schemaName, tableName);
    return tableResult;
  }

  private FileResult createFileResult(VirtualFile file) {
    FileResult fileResult = new FileResult(file);
    return fileResult;
  }

  public List<SourceResult> getSourceResults() {
    return new ArrayList<>(sourceResults.values());
  }

  public long getResourcesCount(){
    return sourceResults.size();
  }

  public double getSuccessRate(){
    long successedSr = sourceResults.values().stream()
            .filter(f -> f.getStatus() == SourceStatus.SUCCESS || f.getStatus() == SourceStatus.SKIPPED)
            .count();
    return getResourcesCount() > 0 ? (double) successedSr / getResourcesCount() * 100 : 0;
  }

  @Deprecated // TODO use lazy result initialization utilities (support multiple table sources)
  public void addSourceResult(SourceResult sourceResult) {
    sourceResults.put(sourceResult.getIdentifier(), sourceResult);
  }

  public void deleteStepFfromShared(PipelineStep pipelineStep) {
    sharedSteps.removeIf((step) -> step.getStep().equals(pipelineStep));
  }
  public void addSharedStep(StepResult stepResult) {
    sharedSteps.add(stepResult);
  }

}

