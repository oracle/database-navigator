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

import com.dbn.common.project.ProjectRef;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.execution.logging.LogOutput;
import com.dbn.execution.logging.LogOutputContext;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Thread-safe ordered buffer for Liquibase console output. */
@Getter
@Setter
public class LogOutputBuffer {
    private final ProjectRef project;
    private final List<LogOutput> output = new ArrayList<>();
    private volatile boolean timestampsEnabled;

    public LogOutputBuffer(@NotNull Project project) {
        this(project, true);
    }

    public LogOutputBuffer(@NotNull Project project, boolean timestampsEnabled) {
        this.project = ProjectRef.of(project);
        this.timestampsEnabled = timestampsEnabled;
    }

    private @NotNull Project getProject() {
        return project.ensure();
    }

    public void appendStdOutput(@Nullable String text) {
        if (Strings.isEmpty(text)) return;

        LogOutput logOutput = LogOutput.createStdOutput(text);
        decorate(logOutput);
        append(logOutput);
    }

    public void appendErrOutput(@Nullable String text) {
        if (Strings.isEmpty(text)) return;

        LogOutput logOutput = LogOutput.createErrOutput(text);
        decorate(logOutput);
        append(logOutput);
    }

    public void appendSysOutput(@NotNull ConnectionHandler connection, @Nullable String text) {
        if (Strings.isEmpty(text)) return;

        LogOutput logOutput = timestampsEnabled ?
                LogOutput.createSysOutput(new LogOutputContext(connection), System.currentTimeMillis(), text, false) :
                LogOutput.createSysOutput(text);
        append(logOutput);
    }

    private void decorate(@NotNull LogOutput logOutput) {
        if (timestampsEnabled) logOutput.withTimestamp(getProject());
    }

    private synchronized void append(@Nullable LogOutput logOutput) {
        if (logOutput == null) return;
        output.add(logOutput);
    }

    @NotNull
    public synchronized List<LogOutput> getOutput() {
        return new ArrayList<>(output);
    }
}
