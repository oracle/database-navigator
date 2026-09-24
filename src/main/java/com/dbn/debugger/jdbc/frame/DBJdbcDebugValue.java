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
import com.dbn.debugger.common.frame.DBDebugValue;
import com.dbn.debugger.jdbc.DBJdbcDebugProcess;
import com.dbn.object.DBType;
import com.dbn.object.DBTypeAttribute;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.xdebugger.frame.XCompositeNode;
import com.intellij.xdebugger.frame.XValueChildrenList;
import com.intellij.xdebugger.frame.XValueModifier;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.List;

public class DBJdbcDebugValue extends DBDebugValue<DBJdbcDebugStackFrame>{
    private DBJdbcDebugValueModifier modifier;
    private final DBObjectRef<DBType> structuredType;

    DBJdbcDebugValue(
            DBJdbcDebugStackFrame stackFrame,
            DBJdbcDebugValue parentValue,
            @NonNls String variableName,
            @Nullable List<String> childVariableNames,
            @Nullable DBType structuredType,
            Icon icon) {
        super(stackFrame, variableName, childVariableNames, parentValue, icon);
        this.structuredType = DBObjectRef.of(structuredType);
    }

    @Override
    public DBJdbcDebugProcess getDebugProcess() {
        return (DBJdbcDebugProcess) super.getDebugProcess();
    }

    @Override
    public XValueModifier getModifier() {
        if (modifier == null) modifier = new DBJdbcDebugValueModifier(this);
        return modifier;
    }

    public boolean isStructured() {
        return structuredType != null;
    }

    @Override
    public boolean hasChildren() {
        DBType type = getStructuredType();
        return type == null ? super.hasChildren() : !type.getAttributes().isEmpty();
    }

    @Override
    public void computeChildren(@NotNull XCompositeNode node) {
        DBType type = getStructuredType();
        if (type == null) {
            super.computeChildren(node);
            return;
        }

        XValueChildrenList children = new XValueChildrenList();
        for (DBTypeAttribute attribute : type.getAttributes()) {
            DBType attributeType = attribute.getDataType().getDeclaredType();
            Icon attributeIcon = attributeType == null ? Icons.DBO_ATTRIBUTE : Icons.DBO_TYPE;
            DBJdbcDebugValue value = getStackFrame().createDebugValue(
                    attribute.getName(), this, null, attributeType, attributeIcon);
            children.add(value);
        }
        node.addChildren(children, true);
    }

    @Nullable
    private DBType getStructuredType() {
        return structuredType == null ? null : structuredType.value();
    }
}
