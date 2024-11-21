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

import com.dbn.common.message.MessageType;
import com.dbn.common.navigation.NavigationInstructions;
import com.dbn.database.DatabaseMessage;
import com.dbn.execution.ExecutionResult;
import com.dbn.execution.compiler.CompilerResult;
import com.dbn.execution.statement.StatementExecutionContext;
import com.dbn.execution.statement.StatementExecutionInput;
import com.dbn.execution.statement.StatementExecutionMessage;
import com.dbn.execution.statement.processor.StatementExecutionProcessor;
import com.dbn.execution.statement.result.ui.StatementExecutionResultForm;

public interface StatementExecutionResult extends ExecutionResult<StatementExecutionResultForm> {
    StatementExecutionProcessor getExecutionProcessor();
    StatementExecutionMessage getExecutionMessage();
    StatementExecutionInput getExecutionInput();
    StatementExecutionContext getExecutionContext();

    StatementExecutionStatus getExecutionStatus();

    void setExecutionStatus(StatementExecutionStatus executionStatus);
    void updateExecutionMessage(MessageType messageType, String message, DatabaseMessage databaseMessage);
    void updateExecutionMessage(MessageType messageType, String message);
    void clearExecutionMessage();
    void calculateExecDuration();
    int getExecutionDuration();



    void navigateToEditor(NavigationInstructions instructions);

    int getUpdateCount();

    CompilerResult getCompilerResult();
    boolean hasCompilerResult();
    boolean isBulkExecution();

    String getLoggingOutput();
    void setLoggingOutput(String loggerOutput);
    boolean isLoggingActive();
    void setLoggingActive(boolean databaseLogActive);
}
