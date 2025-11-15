package com.dbn.vector.model;

import lombok.Getter;

import java.util.Date;

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
  private long startTime;
  private long endTime;

  public StepResult(PipelineStep step) {
    this.step = step;
  }

  public void markSuccess() {
    this.ok = true;
    this.status = STEP_STATUS.SUCCEEDED;
    this.endTime = System.currentTimeMillis();
  }

  public void startAt() {
    this.status = STEP_STATUS.RUNNING;
    startTime = System.currentTimeMillis();
  }

  public void markFailed(String ensureDestError, String message) {
    this.status = STEP_STATUS.FAILED;
    this.errorCode = ensureDestError;
    this.errorMessage = message;
    this.endTime = System.currentTimeMillis();
  }

  public boolean isCritical() {
    return step.isCritical();
  }

  public long getDuration() {
    return endTime - startTime;
  }
}
