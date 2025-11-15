package com.dbn.vector.model;

import com.dbn.common.ui.Presentable;
import com.dbn.vector.model.sourceconfig.SourceType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
@Getter
public abstract class SourceResult implements Presentable {
 protected final SourceType sourceType;
 protected SourceStatus status = SourceStatus.PENDING;
 protected final List<StepResult> steps = new ArrayList<>();
  protected long rowsInserted = 0L;
  protected long durationMs = 0L;
 protected  String displayName;

 protected SourceResult(SourceType sourceType) {
   this.sourceType = sourceType;
 }

 public abstract String getSize();
 public abstract String getIdentifier();

 public StepResult startStep(PipelineStep step) {
   StepResult sr = new StepResult(step);
   sr.startAt();
   steps.add(sr);
   this.status = SourceStatus.RUNNING;
   return sr;
 }

    public void finishFailed(String errorCode, String errorMessage) {
      this.durationMs = steps.stream().mapToLong(StepResult::getDuration).sum();
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
   this.durationMs = steps.stream().mapToLong(StepResult::getDuration).sum();
   this.status = SourceStatus.SUCCESS;
 }

 public void setStatus(SourceStatus status) {
   this.status = status;
 }

}
