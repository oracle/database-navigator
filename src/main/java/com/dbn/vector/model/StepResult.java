package com.dbn.vector.model;

import lombok.Getter;

import javax.swing.*;
import java.util.Date;

@Getter
public class StepResult {
  private String link ="";

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
  private Icon icon ;

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

  public void setLink(String link) {
    this.link = link;
  }

  public void setIcon(Icon icon) {
    this.icon = icon;
  }
}
