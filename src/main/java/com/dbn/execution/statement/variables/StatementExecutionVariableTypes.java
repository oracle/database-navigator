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

package com.dbn.execution.statement.variables;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.connection.ConnectionId;
import com.dbn.data.type.GenericDataType;
import lombok.val;
import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.execution.statement.variables.VariableNames.adjust;

public class StatementExecutionVariableTypes implements PersistentStateElement {
    private final Map<ConnectionId, Map<String, GenericDataType>> variableTypes = new ConcurrentHashMap<>();

    @Nullable
    public GenericDataType getVariableDataType(ConnectionId connectionId, String variableName) {
        val variableTypes = this.variableTypes.get(connectionId);
        if (variableTypes == null) return null;

        variableName = adjust(variableName);
        return variableTypes.get(variableName);
    }

    public void setVariableDataType(ConnectionId connectionId, String variableName, GenericDataType dataType) {
        val variableTypes = this.variableTypes.computeIfAbsent(connectionId, id -> new ConcurrentHashMap<>());

        variableName = adjust(variableName);
        variableTypes.put(variableName, dataType);
    }

    @Override
    public void readState(Element element) {
        Element root = element.getChild("execution-variable-types");
        if (root == null) return;

        for (Element child : root.getChildren()) {
            ConnectionId connectionId = ConnectionId.get(stringAttribute(child, "connection-id"));
            String variableName = adjust(stringAttribute(child, "name"));
            GenericDataType variableType = enumAttribute(child, "data-type", GenericDataType.LITERAL);
            Map<String, GenericDataType> parameters = variableTypes.computeIfAbsent(connectionId, id -> new ConcurrentHashMap<>());
            parameters.put(variableName, variableType);
        }
    }

    @Override
    public void writeState(Element element) {
        Element root = newElement(element, "execution-variable-types");
        for (val entry : variableTypes.entrySet()) {
            ConnectionId connectionId = entry.getKey();
            Map<String, GenericDataType> parameters = entry.getValue();
            for (val paramEntry : parameters.entrySet()) {
                Element child = newElement(root, "variable");
                String parameterName = paramEntry.getKey();
                GenericDataType parameterType = paramEntry.getValue();

                setStringAttribute(child, "connection-id", connectionId.id());
                setStringAttribute(child, "name", parameterName);
                setEnumAttribute(child, "data-type", parameterType);
            }
        }
    }

}
