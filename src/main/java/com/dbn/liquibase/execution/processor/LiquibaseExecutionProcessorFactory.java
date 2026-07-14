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

package com.dbn.liquibase.execution.processor;

import com.dbn.liquibase.execution.LiquibaseExecutionInput;
import com.dbn.liquibase.execution.LiquibaseExecutionProcessor;
import org.jetbrains.annotations.NotNull;

/** Creates the specialized processor for a Liquibase operation. */
public final class LiquibaseExecutionProcessorFactory {
    private LiquibaseExecutionProcessorFactory() {}

    @NotNull
    public static LiquibaseExecutionProcessor create(@NotNull LiquibaseExecutionInput input) {
        return switch (input.getOperation()) {
            case INITIALIZE -> new LiquibaseInitializationProcessor(input);
            case VALIDATE -> new LiquibaseValidationProcessor(input);
            case STATUS -> new LiquibaseStatusProcessor(input);
            case COMPARE, UPDATE, ROLLBACK ->
                throw new UnsupportedOperationException("Unsupported Liquibase operation: " + input.getOperation());
        };
    }
}
