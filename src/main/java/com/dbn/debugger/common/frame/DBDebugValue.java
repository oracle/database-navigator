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

package com.dbn.debugger.common.frame;

import com.dbn.common.icon.Icons;
import com.dbn.common.thread.Background;
import com.dbn.common.util.Strings;
import com.dbn.debugger.common.evaluation.DBDebuggerEvaluator;
import com.dbn.debugger.common.process.DBDebugProcess;
import com.intellij.xdebugger.evaluation.XDebuggerEvaluator;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XNamedValue;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueNode;
import com.intellij.xdebugger.frame.XValuePlace;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.List;

@Getter
@Setter
public abstract class DBDebugValue<T extends DBDebugStackFrame> extends XNamedValue implements Comparable<DBDebugValue>{
    protected String value;
    protected String type;
    protected Icon icon;
    protected List<String> childVariableNames;

    private final T stackFrame;
    private final DBDebugValue<T> parentValue;

    public DBDebugValue(T stackFrame, String name) {
        super(name);
        this.stackFrame = stackFrame;
        this.parentValue = null;
    }

    protected DBDebugValue(T stackFrame, @NotNull String name, @Nullable List<String> childVariableNames, @Nullable DBDebugValue<T>parentValue, @Nullable Icon icon) {
        super(name);
        this.stackFrame = stackFrame;
        this.parentValue = parentValue;
        if (icon == null) {
            icon = parentValue == null ?
                    Icons.DBO_VARIABLE :
                    Icons.DBO_ATTRIBUTE;
        }
        this.icon = icon;
        this.childVariableNames = childVariableNames;
    }

    @Override
    public void computePresentation(@NotNull final XValueNode node, @NotNull final XValuePlace place) {
        // enabling this will show always variables as changed
        //node.setPresentation(icon, null, "", childVariableNames != null);
        Background.run(null, () -> {
            XDebuggerEvaluator evaluator1 = getStackFrame().getEvaluator();
            DBDebuggerEvaluator<? extends DBDebugStackFrame, DBDebugValue> evaluator = (DBDebuggerEvaluator<? extends DBDebugStackFrame, DBDebugValue>) evaluator1;
            evaluator.computePresentation(DBDebugValue.this, node, place);
        });
    }


    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        if (childVariableNames != null) {
            for (String childVariableName : childVariableNames) {
                childVariableName = childVariableName.substring(getVariableName().length() + 1);
                XValueChildrenList debugValueChildren = new XValueChildrenList();
                DBDebugValue value = stackFrame.createDebugValue(childVariableName, this, null, null);
                debugValueChildren.add(value);
                node.addChildren(debugValueChildren, true);
            }
        } else {
            super.computeChildren(node);
        }
    }

    public DBDebugProcess getDebugProcess() {
        return stackFrame.getDebugProcess();
    }

    public String getVariableName() {
        return getName();
    }

    public String getDisplayValue() {
        if (value == null) return childVariableNames == null ? "null" : "";
        return isLiteral() && false ? "'" + value + "'" : value;
    }

    public boolean isNumeric() {
        return value != null && Strings.isNumber(value);
    }

    public boolean isBoolean() {
        return value != null && (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"));
    }

    public boolean isLiteral() {
        return value != null && !value.isEmpty() && !isNumeric() && !isBoolean();
    }

    public boolean hasChildren() {
        return childVariableNames != null;
    }

    @Override
    public int compareTo(@NotNull DBDebugValue remote) {
        return getName().compareTo(remote.getName());
    }
}

