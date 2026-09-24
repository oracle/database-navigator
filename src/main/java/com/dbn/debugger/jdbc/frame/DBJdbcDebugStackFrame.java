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

import com.dbn.common.icon.Icons;
import com.dbn.common.latent.Latent;
import com.dbn.database.common.debug.DebuggerIdentifierInfo;
import com.dbn.database.common.debug.DebuggerIdentifierModel;
import com.dbn.database.common.debug.DebuggerRuntimeInfo;
import com.dbn.database.common.debug.VariableInfo;
import com.dbn.debugger.DBDebugUtil;
import com.dbn.debugger.common.frame.DBDebugSourcePosition;
import com.dbn.debugger.common.frame.DBDebugStackFrame;
import com.dbn.debugger.jdbc.DBJdbcDebugProcess;
import com.dbn.debugger.jdbc.evaluation.DBJdbcDebuggerEvaluator;
import com.dbn.editor.DBContentType;
import com.dbn.execution.ExecutionInput;
import com.dbn.execution.statement.StatementExecutionInput;
import com.dbn.language.common.psi.IdentifierPsiElement;
import com.dbn.object.DBType;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.xdebugger.XSourcePosition;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static com.dbn.common.util.Strings.toLowerCase;

public class DBJdbcDebugStackFrame extends DBDebugStackFrame<DBJdbcDebugProcess, DBJdbcDebugValue> {
    private final DebuggerRuntimeInfo runtimeInfo;
    private final Latent<DBJdbcDebuggerEvaluator> evaluator =
            Latent.basic(() -> new DBJdbcDebuggerEvaluator(DBJdbcDebugStackFrame.this));

    private final Map<String, VariableInfo> variableCache = new ConcurrentHashMap<>();

    DBJdbcDebugStackFrame(DBJdbcDebugProcess debugProcess, DebuggerRuntimeInfo runtimeInfo, int index) {
        super(debugProcess, index);
        this.runtimeInfo = runtimeInfo;
    }

    @SneakyThrows
    public VariableInfo getVariableInfo(String variableName, Function<String, VariableInfo> loader) {
        return variableCache.computeIfAbsent(variableName, loader);
    }

    @Override
    protected void computeValues(List<DBJdbcDebugValue> values) {
        IdentifierPsiElement subject = getSubject();
        VirtualFile virtualFile = getVirtualFile();
        DBSchemaObject object = DBDebugUtil.getObject(getSourcePosition());
        if (object == null || virtualFile == null) {
            super.computeValues(values);
            return;
        }

        DebuggerRuntimeInfo runtimeInfo = this.runtimeInfo;
        int currentLine = runtimeInfo.getLineNumber() == null ? Integer.MAX_VALUE : runtimeInfo.getLineNumber() + 1;
        DBContentType contentType = getSourceContentType();
        DBJdbcDebugProcess debugProcess = getDebugProcess();
        String objectType = debugProcess.getIdentifierObjectType(object, contentType);
        DebuggerIdentifierModel model = debugProcess.getIdentifierModel(object, contentType);
        List<DebuggerIdentifierInfo> variables = subject == null
                ? model.getVisibleVariables(currentLine, objectType)
                : model.getVisibleVariables(subject.getChars().toString(), objectType, currentLine);
        if (variables.isEmpty()) {
            variables = model.getVisibleVariables(currentLine, objectType);
        }
        if (variables.isEmpty()) {
            super.computeValues(values);
            return;
        }

        for (DebuggerIdentifierInfo identifier : variables) {
            values.add(createDebugValue(
                    identifier.getName(),
                    null,
                    null,
                    model.getType(identifier),
                    getIdentifierIcon(identifier, model)));
        }
    }

    private Icon getIdentifierIcon(DebuggerIdentifierInfo identifier, DebuggerIdentifierModel model) {
        if (model.isTypeVariable(identifier)) return Icons.DBO_TYPE;
        if (identifier.isFormalParameter()) return Icons.DBO_ARGUMENT;
        if (identifier.isCursor()) return Icons.DBO_CURSOR;
        return Icons.DBO_VARIABLE;
    }

    private DBContentType getSourceContentType() {
        XSourcePosition sourcePosition = getSourcePosition();
        if (sourcePosition == null) return null;

        VirtualFile sourceFile = sourcePosition.getFile();
        if (sourceFile instanceof DBSourceCodeVirtualFile sourceCodeFile) return sourceCodeFile.getContentType();

        return null;
    }

    @Override
    protected XSourcePosition resolveSourcePosition() {
        DBJdbcDebugProcess debugProcess = getDebugProcess();
        VirtualFile virtualFile = debugProcess.getRuntimeInfoFile(runtimeInfo);

        int lineNumber = runtimeInfo.getLineNumber();
        if (runtimeInfo.getOwnerName() == null) {
            ExecutionInput executionInput = debugProcess.getExecutionInput();
            if (executionInput instanceof StatementExecutionInput statementExecutionInput) {
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
        return createDebugValue(variableName, parentValue, childVariableNames, null, icon);
    }

    DBJdbcDebugValue createDebugValue(String variableName, DBJdbcDebugValue parentValue, List<String> childVariableNames, DBType type, Icon icon) {
        return new DBJdbcDebugValue(this, parentValue, variableName, childVariableNames, type, icon);
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
