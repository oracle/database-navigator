package com.dbn.vector.model;

import lombok.Getter;

@Getter
public class StepResult {
  public enum STEP_STATUS {
    NOT_STARTED,
    RUNNING,
    FAILED,
    SUCCEEDED
  }
  private STEP_STATUS status = STEP_STATUS.NOT_STARTED;
  private PipelineStep step;
  private boolean ok = false;
  private String errorCode;
  private String errorMessage;

  public StepResult(PipelineStep step) {
    this.step = step;
  }

  public void markSuccess() {
    this.ok = true;
    this.status = STEP_STATUS.SUCCEEDED;
  }

  public void markFailed(String ensureDestError, String message) {
    this.status = STEP_STATUS.FAILED;
    this.errorCode = ensureDestError;
    this.errorMessage = message;
  }

  public boolean isCritical() {
    return step.isCritical();
  }
}
