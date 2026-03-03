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

import com.dbn.common.task.Task;
import com.dbn.common.task.TaskStatus;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;

@Getter
@Setter
public class StepResult implements Task {
  private final PipelineStep step;

  private TaskStatus status = TaskStatus.NEW;
  private long startTime;
  private long endTime;

  @NonNls
  private String errorCode;
  private Throwable exception;


  public StepResult(PipelineStep step) {
    this.step = step;
  }

  public void markSuccess() {
    this.status = TaskStatus.DONE;
    this.endTime = System.currentTimeMillis();
  }

  public void start() {
    this.status = TaskStatus.RUNNING;
    this.startTime = System.currentTimeMillis();
  }

  public void markFailed(@NonNls String errorCode, Throwable exception) {
    this.status = TaskStatus.FAILED;
    this.errorCode = errorCode;
    this.exception = exception;
    this.endTime = System.currentTimeMillis();
  }

  public long getDuration() {
    return endTime - startTime;
  }
}
