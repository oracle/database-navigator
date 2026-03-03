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

import com.dbn.common.task.TaskStatus;
import com.dbn.common.ui.Presentable;
import com.dbn.vector.model.request.EmbeddingSource;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public abstract class EmbeddingResult<T extends EmbeddingSource> implements Presentable {
    private final T source;
    private TaskStatus status = TaskStatus.NEW;

    private final List<StepResult> steps;
    private long rowsInserted = 0L;
    private Duration duration;
    private String displayName;
    private String metadata;

    @NonNls
    private String errorCode;
    private Throwable exception;


    protected EmbeddingResult(T source) {
        this.source = source;
        this.steps = new ArrayList<>(initSteps());
    }

    protected abstract List<StepResult> initSteps();

    public abstract String getPresentableSize();

    public abstract String getIdentifier();

    public StepResult startStep(PipelineStep stepType) {
        StepResult step = getStep(stepType);
        step.start();
        this.status = TaskStatus.RUNNING;
        return step;
    }

    public StepResult getStep(PipelineStep step) {
        for (StepResult stepResult : steps) {
            if (stepResult.getStep().equals(step)) {
                return stepResult;
            }
        }
        return null;
    }

    public StepResult deleteStep(PipelineStep step) {
        for (int i = 0; i < steps.size(); i++) {
            StepResult stepResult = steps.get(i);
            if (stepResult.getStep().equals(step)) {
                return steps.remove(i);
            }
        }
        return null;
    }

    public void finishFailed(@NonNls String errorCode, Throwable exception) {
        this.duration = Duration.ofMillis(steps.stream().mapToLong(StepResult::getDuration).sum());
        this.status = TaskStatus.FAILED;
        this.errorCode = errorCode;
        this.exception = exception;
    }

    public String getErrorMessage() {
        return steps.stream()
                .filter(s -> s.getStatus() == TaskStatus.FAILED)
                .findFirst()
                .map(r -> r.getException().getMessage())
                .orElse(null);
    }

    public void finishSuccess(long rowsInserted) {
        this.rowsInserted = rowsInserted;
        this.duration = Duration.ofMillis(steps.stream().mapToLong(StepResult::getDuration).sum());
        this.status = TaskStatus.FINISHED;
    }

    public String getSourceTooltip() {
        return null;
    }

    public String getStatusTooltip() {
        return null;
    }

}
