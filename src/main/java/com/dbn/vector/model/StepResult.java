package com.dbn.vector.model;

import lombok.Getter;
import lombok.Setter;

import javax.swing.Icon;

@Getter
@Setter
public class StepResult {


  public enum STEP_STATUS {
    NOT_STARTED,
    RUNNING,
    FAILED,
    SUCCEEDED
  }
  private STEP_STATUS status = STEP_STATUS.NOT_STARTED;
  private PipelineStep step;
  private String errorCode;
  private String errorMessage;
  private long startTime;
  private long endTime;
  private Icon icon ;
  private String link ="";

  public StepResult(PipelineStep step) {
    this.step = step;
  }

  public void markSuccess() {
    this.status = STEP_STATUS.SUCCEEDED;
    this.endTime = System.currentTimeMillis();
  }

  public void start() {
    this.status = STEP_STATUS.RUNNING;
    startTime = System.currentTimeMillis();
  }

  public void markFailed(String ensureDestError, String message) {
    this.status = STEP_STATUS.FAILED;
    this.errorCode = ensureDestError;
    this.errorMessage = message;
    this.endTime = System.currentTimeMillis();
  }

  public boolean isOk() {
    return errorCode == null;
  }

  public boolean isCritical() {
    return step.isCritical();
  }

  public long getDuration() {
    return endTime - startTime;
  }
}
