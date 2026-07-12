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

package com.dbn.liquibase.execution.logging;

import com.dbn.liquibase.execution.LiquibaseExecutionResult;

import liquibase.logging.core.AbstractLogService;
import liquibase.logging.core.AbstractLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Level;

/** Forwards Liquibase log messages to a DBN execution result. */
public class LiquibaseExecutionLogService extends AbstractLogService {
    private final LiquibaseExecutionResult result;
    private final AbstractLogger logger = new AbstractLogger() {
        @Override
        public void log(@NotNull Level level, @Nullable String message, @Nullable Throwable exception) {
            append(level, message, exception);
        }
    };

    public LiquibaseExecutionLogService(@NotNull LiquibaseExecutionResult result) {
        this.result = result;
    }

    @Override
    public int getPriority() {
        return Integer.MAX_VALUE;
    }

    @NotNull
    @Override
    public liquibase.logging.Logger getLog(@NotNull Class clazz) {
        return logger;
    }

    private void append(@NotNull Level level, @Nullable String message, @Nullable Throwable exception) {
        String text = message == null ? "" : message;
        if (exception != null) {
            StringWriter output = new StringWriter();
            exception.printStackTrace(new PrintWriter(output));
            if (!text.isEmpty()) text += System.lineSeparator();
            text += output;
        }
        if (text.isEmpty()) return;

        if (level.intValue() >= Level.SEVERE.intValue()) {
            result.appendErrorOutput(text);
        } else {
            result.appendInfoOutput(level == Level.INFO ? text : level.getName() + ": " + text);
        }
    }
}
