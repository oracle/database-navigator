/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.migration.flyway.execution.processor;

import com.dbn.common.extension.ExtensionPointCache;
import com.dbn.migration.flyway.execution.FlywayExecutionProcessor;
import com.dbn.migration.flyway.operation.FlywayOperation;
import org.jetbrains.annotations.NotNull;

/**
 * Resolves the specialized processor for a Flyway operation.
 */
public final class FlywayExecutionProcessors extends ExtensionPointCache<FlywayOperation, FlywayExecutionProcessor> {
    private static final FlywayExecutionProcessors INSTANCE = new FlywayExecutionProcessors();

    private FlywayExecutionProcessors() {
        super(FlywayExecutionProcessor.EP, FlywayExecutionProcessor::getOperation);
    }

    @NotNull
    public static FlywayExecutionProcessor get(@NotNull FlywayOperation operation) {
        return INSTANCE.find(operation);
    }
}
