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

package com.dbn.debugger.jdbc.evaluation;

import com.dbn.common.util.Commons;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.debug.VariableInfo;
import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.debugger.common.evaluation.DBDebuggerEvaluator;
import com.dbn.debugger.common.frame.DBDebugValue;
import com.dbn.debugger.jdbc.DBJdbcDebugProcess;
import com.dbn.debugger.jdbc.frame.DBJdbcDebugStackFrame;
import com.dbn.debugger.jdbc.frame.DBJdbcDebugValue;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import com.intellij.xdebugger.frame.presentation.XNumericValuePresentation;
import com.intellij.xdebugger.frame.presentation.XStringValuePresentation;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.List;

import static com.dbn.common.util.Strings.toLowerCase;
import static com.dbn.common.util.Strings.toUpperCase;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DBJdbcDebuggerEvaluator extends DBDebuggerEvaluator<DBJdbcDebugStackFrame, DBJdbcDebugValue> {

    public DBJdbcDebuggerEvaluator(DBJdbcDebugStackFrame frame) {
        super(frame);
    }

    @Override
    public void computePresentation(@NotNull DBJdbcDebugValue debugValue, @NotNull final XValueNode node, @NotNull XValuePlace place) {
        List<String> childVariableNames = debugValue.getChildVariableNames();

        try {
            DBJdbcDebugProcess debugProcess = debugValue.getDebugProcess();
            String variableName = debugValue.getVariableName();
            DBDebugValue parentValue = debugValue.getParentValue();
            String dbVariableName = parentValue == null ? variableName : parentValue.getVariableName() + "." + variableName;
            dbVariableName = toUpperCase(dbVariableName);
            int frameIndex = debugValue.getStackFrame().getFrameIndex();

            DBNConnection conn = debugProcess.getDebuggerConnection();
            DatabaseDebuggerInterface debuggerInterface = debugProcess.getDebuggerInterface();

            VariableInfo variableInfo = debuggerInterface.getVariableInfo(dbVariableName, frameIndex, conn);
            if (variableInfo.getError() != null && frameIndex > 0) {
                // TODO why is the variable lookup not following the "one based" frame indexing?
                variableInfo = debuggerInterface.getVariableInfo(dbVariableName, frameIndex - 1, conn);
            }

            String value = variableInfo.getValue();
            String type = variableInfo.getError();

            if (type != null) {
                type = toLowerCase(type);
                value = "";
            }
            if (childVariableNames != null) {
                type = "record";
            }

            debugValue.setValue(value);
            debugValue.setType(type);
        } catch (Exception e) {
            conditionallyLog(e);
            debugValue.setValue("");
            debugValue.setType(e.getMessage());
        } finally {
            updateValuePresentation(debugValue, node);
        }
    }

    private static void updateValuePresentation(@NotNull DBJdbcDebugValue debugValue, @NotNull XValueNode node) {
        Icon icon = debugValue.getIcon();
        String value = debugValue.getDisplayValue();
        boolean hasChildren = debugValue.hasChildren();

        if (debugValue.isNumeric()) {
            node.setPresentation(icon, new XNumericValuePresentation(value), hasChildren);
        } else if (debugValue.isLiteral()) {
            node.setPresentation(icon, new XStringValuePresentation(value), hasChildren);
        } else {
            node.setPresentation(
                    icon,
                    debugValue.getType(),
                    Commons.nvl(value, "null"),
                    hasChildren);
        }
    }
}
