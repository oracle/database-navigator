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

package com.dbn.execution.statement.result;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.icon.Icons;
import com.dbn.common.message.MessageType;
import com.dbn.common.navigation.NavigationInstructions;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.SchemaId;
import com.dbn.database.DatabaseMessage;
import com.dbn.execution.ExecutionResultBase;
import com.dbn.execution.compiler.CompilerResult;
import com.dbn.execution.statement.StatementExecutionContext;
import com.dbn.execution.statement.StatementExecutionInput;
import com.dbn.execution.statement.StatementExecutionMessage;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.execution.statement.result.ui.StatementExecutionResultForm;
import com.dbn.language.common.DBLanguagePsiFile;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

import static com.dbn.common.dispose.Disposer.replace;

@Getter
@Setter
public class StatementExecutionBasicResult extends ExecutionResultBase<StatementExecutionResultForm> implements StatementExecutionResult{
    private String name;
    private StatementExecutionMessage executionMessage;
    private StatementExecutionStatus executionStatus;
    private int executionDuration;
    private CompilerResult compilerResult;
    private String loggingOutput;
    private boolean loggingActive;

    private StatementExecutionProcessor executionProcessor;
    private final ConnectionRef connection;
    private final SchemaId databaseSchema;
    private final int updateCount;

    public StatementExecutionBasicResult(StatementExecutionProcessor executionProcessor, String name, int updateCount) {
        this.name = name;
        this.updateCount = updateCount;
        this.connection = Failsafe.nn(executionProcessor.getConnection()).ref();
        this.databaseSchema = executionProcessor.getTargetSchema();
        this.executionProcessor = executionProcessor;
    }

    @Override
    public DBLanguagePsiFile createPreviewFile() {
        return getExecutionInput().createPreviewFile();
    }

    @Override
    public void setName(@NotNull String name, boolean sticky) {
        this.name = name;
        getExecutionProcessor().setResultName(name, sticky);
    }

    @Override
    public Icon getIcon() {
        return getExecutionProcessor().isDirty() ?
                Icons.STMT_EXEC_RESULTSET_ORPHAN :
                Icons.STMT_EXEC_RESULTSET;
    }

    @Override
    @NotNull
    public StatementExecutionProcessor getExecutionProcessor() {
        return Failsafe.nn(executionProcessor);
    }

    @Override
    public StatementExecutionMessage getExecutionMessage() {
        return executionMessage;
    }

    @Override
    @NotNull
    public StatementExecutionInput getExecutionInput() {
        return getExecutionProcessor().getExecutionInput();
    }

    @NotNull
    @Override
    public StatementExecutionContext getExecutionContext() {
        return getExecutionInput().getExecutionContext();
    }

    @Override
    public void navigateToEditor(NavigationInstructions instructions) {
          getExecutionProcessor().navigateToEditor(instructions);
    }

    @Override
    public void calculateExecDuration() {
        this.executionDuration = (int) (System.currentTimeMillis() - getExecutionContext().getExecutionTimestamp());
    }

    @Override
    public void updateExecutionMessage(MessageType messageType, String message, DatabaseMessage databaseMessage) {
        executionMessage = new StatementExecutionMessage(this, message, databaseMessage, messageType);
    }

    @Override
    public void updateExecutionMessage(MessageType messageType, String message) {
        executionMessage = new StatementExecutionMessage(this, message, null, messageType);
    }

    @Override
    public void clearExecutionMessage() {
        executionMessage = replace(executionMessage, null);
    }

    @Override
    @NotNull
    public Project getProject() {
        return getExecutionProcessor().getProject();
    }

    @Override
    public ConnectionId getConnectionId() {
        return getExecutionInput().getConnectionId();
    }

    @Override
    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Nullable
    @Override
    public StatementExecutionResultForm createForm() {
        return null;
    }

    @Override
    public boolean hasCompilerResult() {
        return compilerResult != null;
    }

    @Override
    public boolean isBulkExecution() {
        return getExecutionInput().isBulkExecution();
    }

    @Override
    public void disposeInner() {
        executionProcessor = null;
        super.disposeInner();
    }
}
