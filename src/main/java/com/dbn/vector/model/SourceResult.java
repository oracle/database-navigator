package com.dbn.vector.model;

import com.dbn.common.ui.Presentable;
import com.dbn.vector.model.sourceconfig.SourceType;
import lombok.Getter;

import java.util.List;
@Getter
public abstract class SourceResult implements Presentable {
 protected final SourceType sourceType;
 protected SourceStatus status = SourceStatus.FAILED;
 protected  List<StepResult> steps ;
  protected long rowsInserted = 0L;
  protected long durationMs = 0L;
 protected  String displayName;

 protected SourceResult(SourceType sourceType) {
   this.sourceType = sourceType;
 }

 public abstract String getSize();
 public abstract String getIdentifier();

 public StepResult startStep(PipelineStep stepType) {
   StepResult step = getStep(stepType);
   step.start();
   this.status = SourceStatus.RUNNING;
   return step;
 }

  public StepResult getStep(PipelineStep step) {
    for (StepResult stepResult : steps) {
      if (stepResult.getStep().equals(step) ){
        return stepResult;
      }
    }
    return null;
  }

  public StepResult deleteStep(PipelineStep step) {
    for (int i = 0; i < steps.size() ; i++) {
      StepResult stepResult = steps.get(i);
      if (stepResult.getStep().equals(step)) {
        return steps.remove(i);
      }
    }
    return null;
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
