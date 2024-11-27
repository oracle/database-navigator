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

package com.dbn.execution.statement;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.message.MessageType;
import com.dbn.connection.ConnectionId;
import com.dbn.database.DatabaseMessage;
import com.dbn.execution.common.message.ConsoleMessage;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.execution.statement.result.StatementExecutionResult;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;

import static com.dbn.common.dispose.Checks.isNotValid;

@Getter
@Setter
public class StatementExecutionMessage extends ConsoleMessage {
    private StatementExecutionResult executionResult;
    private final DatabaseMessage databaseMessage;
    private final ConnectionId connectionId;

    public StatementExecutionMessage(StatementExecutionResult executionResult, String message, DatabaseMessage databaseMessage, MessageType messageType) {
        super(messageType, message);
        this.executionResult = executionResult;
        this.connectionId = executionResult.getConnectionId();
        this.databaseMessage = databaseMessage;
    }

    public VirtualFile getVirtualFile() {
        VirtualFile virtualFile = executionResult.getExecutionProcessor().getVirtualFile();
        return Failsafe.nn(virtualFile);
    }

    public boolean isOrphan() {
        if (isNotValid(executionResult)) return true;
        StatementExecutionProcessor executionProcessor = executionResult.getExecutionProcessor();
        return executionProcessor.isDirty() ||
                executionProcessor.getExecutionResult() != executionResult; // overwritten result
    }

    @Override
    public boolean isNew() {
        return super.isNew()/* && !isOrphan()*/;
    }

    public void createStatementViewer() {
        
    }

    @Override
    public void disposeInner() {
        Disposer.dispose(executionResult);
        super.disposeInner();
    }
}
