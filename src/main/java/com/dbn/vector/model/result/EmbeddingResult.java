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

import com.dbn.common.ui.Presentable;
import com.dbn.vector.model.request.EmbeddingSource;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public abstract class EmbeddingResult<T extends EmbeddingSource> implements Presentable {
    private final T source;

    private SourceStatus status = SourceStatus.FAILED;
    private List<StepResult> steps;
    private long rowsInserted = 0L;
    private long durationMs = 0L;
    private String displayName;
    private String metadata;

    protected EmbeddingResult(T source) {
        this.source = source;
    }

    public abstract String getPresentableSize();

    public abstract String getIdentifier();

    public StepResult startStep(PipelineStep stepType) {
        StepResult step = getStep(stepType);
        step.start();
        this.status = SourceStatus.RUNNING;
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

    public void finishFailed(String errorCode, String errorMessage) {
        this.durationMs = steps.stream().mapToLong(StepResult::getDuration).sum();
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
        this.durationMs = steps.stream().mapToLong(StepResult::getDuration).sum();
        this.status = SourceStatus.SUCCESS;
    }
}
