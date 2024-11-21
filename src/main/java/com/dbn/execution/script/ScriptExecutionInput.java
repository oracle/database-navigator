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

package com.dbn.execution.script;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.execution.ExecutionTarget;
import com.dbn.execution.RemoteExecutionInput;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScriptExecutionInput extends RemoteExecutionInput {
    private CmdLineInterface cmdLineInterface;
    private VirtualFile sourceFile;
    private boolean clearOutput;

    ScriptExecutionInput(Project project, VirtualFile sourceFile, ConnectionHandler connection, SchemaId targetSchema, boolean clearOutput) {
        super(project, ExecutionTarget.SCRIPT);
        this.sourceFile = sourceFile;
        setTargetConnection(connection);
        setTargetSchemaId(targetSchema);
        this.clearOutput = clearOutput;
    }

    @Override
    protected ScriptExecutionContext createExecutionContext() {
        return new ScriptExecutionContext(this);
    }

    @Override
    public ConnectionHandler getConnection() {
        return getTargetConnection();
    }

    public SchemaId getSchemaId() {
        return getTargetSchemaId();
    }
}
