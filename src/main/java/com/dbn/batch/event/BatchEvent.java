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

package com.dbn.batch.event;

import com.dbn.batch.Batch;
import com.dbn.batch.BatchTask;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class BatchEvent {
    private final Batch batch;
    private final BatchTask task;
    private final BatchEventType type;

    public BatchEvent(@NotNull BatchEventType type, @NotNull Batch batch, @Nullable BatchTask task) {
        this.batch = batch;
        this.task = task;
        this.type = type;
    }
}
