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

package com.dbn.debugger.jdbc.frame;

import com.dbn.common.latent.Latent;
import com.dbn.database.common.debug.DebuggerRuntimeInfo;
import com.dbn.debugger.common.frame.DBDebugSourcePosition;
import com.dbn.debugger.common.frame.DBDebugStackFrame;
import com.dbn.debugger.jdbc.DBJdbcDebugProcess;
import com.dbn.debugger.jdbc.evaluation.DBJdbcDebuggerEvaluator;
import com.dbn.execution.ExecutionInput;
import com.dbn.execution.statement.StatementExecutionInput;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XSourcePosition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.List;

import static com.dbn.common.util.Strings.toLowerCase;

public class DBJdbcDebugStackFrame extends DBDebugStackFrame<DBJdbcDebugProcess, DBJdbcDebugValue> {
    private final DebuggerRuntimeInfo runtimeInfo;
    private final Latent<DBJdbcDebuggerEvaluator> evaluator =
            Latent.basic(() -> new DBJdbcDebuggerEvaluator(DBJdbcDebugStackFrame.this));

    DBJdbcDebugStackFrame(DBJdbcDebugProcess debugProcess, DebuggerRuntimeInfo runtimeInfo, int index) {
        super(debugProcess, index);
        this.runtimeInfo = runtimeInfo;
    }

    @Override
    protected XSourcePosition resolveSourcePosition() {
        DBJdbcDebugProcess debugProcess = getDebugProcess();
        VirtualFile virtualFile = debugProcess.getRuntimeInfoFile(runtimeInfo);

        int lineNumber = runtimeInfo.getLineNumber();
        if (runtimeInfo.getOwnerName() == null) {
            ExecutionInput executionInput = debugProcess.getExecutionInput();
            if (executionInput instanceof StatementExecutionInput) {
                StatementExecutionInput statementExecutionInput = (StatementExecutionInput) executionInput;
                lineNumber += statementExecutionInput.getExecutableLineNumber();
            }
        }
        return DBDebugSourcePosition.create(virtualFile, lineNumber);
    }

    @Override
    protected VirtualFile resolveVirtualFile() {
        return getDebugProcess().getRuntimeInfoFile(runtimeInfo);
    }

    @NotNull
    @Override
    public DBJdbcDebugValue createDebugValue(String variableName, DBJdbcDebugValue parentValue, List<String> childVariableNames, Icon icon) {
        return new DBJdbcDebugValue(this, parentValue, variableName, childVariableNames, icon);
    }

    @Nullable
    @Override
    protected DBJdbcDebugValue createSuspendReasonDebugValue() {
        return new DBSuspendReasonDebugValue(this);
    }

    @Override
    @NotNull
    public DBJdbcDebuggerEvaluator getEvaluator() {
        return evaluator.get();
    }


    @Nullable
    @Override
    public Object getEqualityObject() {
        DebuggerRuntimeInfo runtimeInfo = getDebugProcess().getRuntimeInfo();
        IdentifierPsiElement subject = getSubject();
        String subjectString = subject == null ? null : subject.getText();
        return runtimeInfo == null ? null : toLowerCase(runtimeInfo.getOwnerName() + "." + runtimeInfo.getProgramName() + "." + subjectString);
    }
}


