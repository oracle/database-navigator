package com.dbn.vector.model;

import com.dbn.common.ui.Presentable;
import com.dbn.vector.model.sourceconfig.SourceType;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
@Getter
public abstract class SourceResult implements Presentable {
  private final SourceType sourceType;
  private SourceStatus status = SourceStatus.PENDING;
  private final List<StepResult> steps = new ArrayList<>();
  private long rowsInserted = 0L;
//  protected long durationMs = 0L;

 protected SourceResult(SourceType sourceType) {
   this.sourceType = sourceType;
 }

 public abstract String getSize();

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
