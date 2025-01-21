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

package com.dbn.database.oracle.execution;

import com.dbn.execution.common.options.ExecutionEngineSettings;
import com.dbn.execution.method.options.MethodExecutionSettings;
import com.dbn.object.DBMethod;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;

public class OracleMethodDebugExecutionProcessor extends OracleMethodExecutionProcessor {
    public OracleMethodDebugExecutionProcessor(DBMethod method) {
        super(method);
    }

    @Override
    protected void preHookExecutionCommand(@NonNls StringBuilder buffer) {
        super.preHookExecutionCommand(buffer);
        DBMethod method = getMethod();
        Project project = method.getProject();
        MethodExecutionSettings methodExecutionSettings = ExecutionEngineSettings.getInstance(project).getMethodExecutionSettings();
        int debugExecutionTimeout = methodExecutionSettings.getDebugExecutionTimeout();
        if (debugExecutionTimeout > 0) {
            buffer.append("\n    v_timeout := SYS.DBMS_DEBUG.set_timeout(").append(debugExecutionTimeout).append(");\n");
        }
    }

    @Override
    protected void postHookExecutionCommand(@NonNls StringBuilder buffer) {
        buffer.append("\n");
        buffer.append("    SYS.DBMS_DEBUG.debug_off();\n");
        buffer.append("exception\n");
        buffer.append("    when others then\n");
        buffer.append("        SYS.DBMS_DEBUG.debug_off();\n");
        buffer.append("        raise;\n");
    }

}