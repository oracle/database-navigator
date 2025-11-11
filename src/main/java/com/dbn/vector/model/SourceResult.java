package com.dbn.vector.model;

import com.dbn.vector.model.sourceconfig.SourceType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
@Getter
public abstract class SourceResult  {
 protected final SourceType sourceType;
 protected SourceStatus status = SourceStatus.PENDING;
 protected final List<StepResult> steps = new ArrayList<>();
  protected long rowsInserted = 0L;
//  protected long durationMs = 0L;
 protected  String displayName; // nice label for UI (filename or table name)

 protected SourceResult(SourceType sourceType) {
   this.sourceType = sourceType;
 }

 public SourceType getSourceType() { return sourceType; }
 public SourceStatus getStatus() { return status; }
 public List<StepResult> getSteps() { return steps; }
//  public long getRowsInserted() { return rowsInserted; }
//  public long getDurationMs() { return durationMs; }
//  public String getDisplayName() { return displayName; }

 public StepResult startStep(PipelineStep step) {
   StepResult sr = new StepResult(step);
   steps.add(sr);
   this.status = SourceStatus.RUNNING;
   return sr;
 }

    public void finishFailed(String errorCode, String errorMessage) {
//      this.durationMs = steps.stream().mapToLong(StepResult::durationMs).sum();
      this.status = SourceStatus.FAILED;
      //todo clean up??
    }

    public void setDisplayName(String displayName) {
      this.displayName = displayName;
    }
    public String getErrorMessage() {
        return steps.stream()
                .filter(s -> s.getStatus() == StepResult.STEP_STATUS.FAILED)
                .findFirst()
                .map(StepResult::getErrorMessage)
                .orElse(null);
    }
 public void finishSuccess(long rowsInserted) {
   this.rowsInserted = rowsInserted;
//   this.durationMs = steps.stream().mapToLong(StepResult::durationMs).sum();
   this.status = SourceStatus.SUCCESS;
 }

 public void finishSuccess(PipelineStep step) {

 }
}
