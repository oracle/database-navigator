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

package com.dbn.execution.common.input;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.config.ConnectionConfigListener;
import lombok.val;
import org.jdom.Element;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.options.setting.Settings.connectionIdAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;

public class ExecutionVariableHistory implements PersistentStateElement, ConnectionConfigListener {
    private final Map<ConnectionId, Map<String, ExecutionVariable>> executionVariables = new ConcurrentHashMap<>();

    public ExecutionVariable getExecutionVariable(ConnectionId connectionId, String name, boolean create) {
        Map<String, ExecutionVariable> variables = this.executionVariables.get(connectionId);

        if (variables != null) {
            for (String variable : variables.keySet()) {
                if (Strings.equalsIgnoreCase(variable, name)) {
                    return variables.get(variable);
                }
            }
        }

        if (create) {
            if (variables == null) {
                variables = new HashMap<>();
                this.executionVariables.put(connectionId, variables);
            }

            ExecutionVariable variable = new ExecutionVariable(name);
            variables.put(name, variable);
            return variable;

        }
        return null;
    }

    public void cacheVariable(ConnectionId connectionId, String name, String value) {
        if (Strings.isNotEmpty(value)) {
            ExecutionVariable executionVariable = getExecutionVariable(connectionId, name, true);
            executionVariable.setValue(value);
        }
    }

    public void connectionRemoved(ConnectionId connectionId) {
        executionVariables.remove(connectionId);
    }

    /*********************************************
     *            PersistentStateElement         *
     *********************************************/
    @Override
    public void readState(Element element) {
        Element variablesCacheElement = element.getChild("execution-variables-cache");
        if (variablesCacheElement != null) {
            this.executionVariables.clear();
            for (Element connectionElement : variablesCacheElement.getChildren()) {
                ConnectionId connectionId = connectionIdAttribute(connectionElement, "connection-id");
                for (Element variablesElement : connectionElement.getChildren()) {
                    String path = stringAttribute(variablesElement, "path");
                    ExecutionVariable variable = getExecutionVariable(connectionId, path, true);
                    variable.readState(variablesElement);
                }
            }
        }
    }

    @Override
    public void writeState(Element element) {
        Element variablesCacheElement = newElement(element, "execution-variables-cache");

        for (val entry : executionVariables.entrySet()) {
            ConnectionId connectionId = entry.getKey();
            Element connectionElement = newElement(variablesCacheElement, "connection");
            connectionElement.setAttribute("connection-id", connectionId.id());

            for (ExecutionVariable variable : entry.getValue().values()) {
                if (!variable.getValueHistory().isEmpty()) {
                    Element variableElement = newElement(connectionElement, "variable");
                    variable.writeState(variableElement);
                }

            }
        }
    }
}
