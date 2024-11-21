/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.debugger.jdwp;

import com.dbn.debugger.DBDebuggerType;
import com.dbn.debugger.common.process.DBProgramRunner;
import com.dbn.debugger.common.state.DBRunProfileState;
import com.intellij.debugger.engine.RemoteDebugProcessHandler;
import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.openapi.project.Project;
import com.sun.jdi.Location;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
public final class DBJdwpDebugUtil {

    @Nullable
    public static String getOwnerName(@Nullable Location location) {
        try {
            if (location != null) {
                String sourceUrl = location.sourcePath();
                DBJdwpSourcePath sourcePath = DBJdwpSourcePath.from(sourceUrl);
                return sourcePath.getProgramOwner();
            }
        } catch (Exception e) {
            conditionallyLog(e);
            log.error("Failed to resolve owner name", e);
        }

        return null;
    }

    public static ExecutionResult execute(DBRunProfileState state, Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
        if (runner instanceof DBProgramRunner) {
            DBProgramRunner<?> programRunner = (DBProgramRunner<?>) runner;
            if (programRunner.getDebuggerType() == DBDebuggerType.JDWP) {
                Project project = state.getEnvironment().getProject();
                RemoteDebugProcessHandler processHandler = new RemoteDebugProcessHandler(project);
                return new DefaultExecutionResult(null, processHandler);
            }
        }
        return null;
    }
}
