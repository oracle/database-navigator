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

package com.dbn.execution.statement.processor;

import com.dbn.common.message.MessageType;
import com.dbn.connection.jdbc.DBNResultSet;
import com.dbn.connection.jdbc.DBNStatement;
import com.dbn.execution.statement.StatementExecutionInput;
import com.dbn.execution.statement.result.StatementExecutionCursorResult;
import com.dbn.execution.statement.result.StatementExecutionResult;
import com.dbn.execution.statement.result.StatementExecutionStatus;
import com.dbn.language.common.DBLanguagePsiFile;
import com.dbn.language.common.psi.ExecutablePsiElement;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;

public class StatementExecutionCursorProcessor extends StatementExecutionBasicProcessor {

    public StatementExecutionCursorProcessor(@NotNull Project project, @NotNull FileEditor fileEditor, @NotNull ExecutablePsiElement psiElement, int index) {
        super(project, fileEditor, psiElement, index);
    }

    public StatementExecutionCursorProcessor(@NotNull Project project, @NotNull FileEditor fileEditor, @NotNull DBLanguagePsiFile file, String sqlStatement, int index) {
        super(project, fileEditor, file, sqlStatement,  index);
    }

    @Override
    @NotNull
    protected StatementExecutionResult createExecutionResult(DBNStatement statement, StatementExecutionInput executionInput) throws SQLException {
        DBNResultSet resultSet = statement.getResultSet();
        int updateCount = statement.getUpdateCount();
        String resultName = getResultName();
        if (resultSet == null) {
            statement.close();

            StatementExecutionResult executionResult = new StatementExecutionCursorResult(this, resultName, updateCount);
            executionResult.updateExecutionMessage(MessageType.INFO, getStatementName() + " executed successfully.");
            executionResult.setExecutionStatus(StatementExecutionStatus.SUCCESS);
            return executionResult;
        } else {
            StatementExecutionResult executionResult = getExecutionResult();
            if (executionResult == null) {
                executionResult = new StatementExecutionCursorResult(this, resultName, resultSet, updateCount);
                executionResult.setExecutionStatus(StatementExecutionStatus.SUCCESS);
                return executionResult;
            } else {
                // if executionResult exists, just update it with the new resultSet data
                if (executionResult instanceof StatementExecutionCursorResult){
                    StatementExecutionCursorResult executionCursorResult = (StatementExecutionCursorResult) executionResult;
                    executionCursorResult.loadResultSet(resultSet);
                    return executionResult;
                } else {
                    return new StatementExecutionCursorResult(this, resultName, resultSet, updateCount);
                }
            }
        }

    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public boolean canExecute() {
        if (super.canExecute()) {
            StatementExecutionResult executionResult = getExecutionResult();
            return executionResult == null ||
                    executionResult.getExecutionStatus() == StatementExecutionStatus.ERROR ||
                    executionResult.getExecutionProcessor().isDirty();
        }
        return false;
    }

    @Override
    public void navigateToResult() {
        StatementExecutionResult executionResult = getExecutionResult();
        if (executionResult instanceof StatementExecutionCursorResult) {
            StatementExecutionCursorResult executionCursorResult = (StatementExecutionCursorResult) executionResult;
            executionCursorResult.navigateToResult();
        }

    }

    @Override
    public boolean isQuery() {
         return true;
    }
}
