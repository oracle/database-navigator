/*
 * Copyright 2025 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.vector.model.result;

import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;

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

  public void markFailed(@NonNls String ensureDestError, String message) {
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
