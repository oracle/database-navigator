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

package com.dbn.execution.java;

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

public class JavaExecutionArgumentValueHistory implements PersistentStateElement, ConnectionConfigListener {
    private final Map<ConnectionId, Map<String, JavaExecutionArgumentValue>> argumentValues = new ConcurrentHashMap<>();

    public JavaExecutionArgumentValue getArgumentValue(ConnectionId connectionId, String name, boolean create) {
        Map<String, JavaExecutionArgumentValue> argumentValues = this.argumentValues.get(connectionId);

        if (argumentValues != null) {
            for (String argumentName : argumentValues.keySet()) {
                if (Strings.equalsIgnoreCase(argumentName, name)) {
                    return argumentValues.get(argumentName);
                }
            }
        }

        if (create) {
            if (argumentValues == null) {
                argumentValues = new HashMap<>();
                this.argumentValues.put(connectionId, argumentValues);
            }

            JavaExecutionArgumentValue argumentValue = new JavaExecutionArgumentValue(name);
            argumentValues.put(name, argumentValue);
            return argumentValue;

        }
        return null;
    }

    public void cacheVariable(ConnectionId connectionId, String name, String value) {
        if (Strings.isNotEmpty(value)) {
            JavaExecutionArgumentValue argumentValue = getArgumentValue(connectionId, name, true);
            argumentValue.setValue(value);
        }
    }

    public void connectionRemoved(ConnectionId connectionId) {
        argumentValues.remove(connectionId);
    }

    /*********************************************
     *            PersistentStateElement         *
     *********************************************/
    @Override
    public void readState(Element element) {
        Element argumentValuesElement = element.getChild("argument-values-cache");
        if (argumentValuesElement != null) {
            this.argumentValues.clear();
            for (Element argumentValueElement : argumentValuesElement.getChildren()) {
                ConnectionId connectionId = connectionIdAttribute(argumentValueElement, "connection-id");
                for (Element argumentElement : argumentValueElement.getChildren()) {
                    String name = stringAttribute(argumentElement, "name");
                    JavaExecutionArgumentValue argumentValue = getArgumentValue(connectionId, name, true);
                    argumentValue.readState(argumentElement);
                }
            }
        }
    }

    @Override
    public void writeState(Element element) {
        Element argumentValuesElement = newElement(element, "argument-values-cache");

        for (val entry : argumentValues.entrySet()) {
            ConnectionId connectionId = entry.getKey();
            Element connectionElement = newElement(argumentValuesElement, "connection");
            connectionElement.setAttribute("connection-id", connectionId.id());

            for (val argumentEntry : entry.getValue().entrySet()) {
                JavaExecutionArgumentValue argumentValue = argumentEntry.getValue();
                if (!argumentValue.getValueHistory().isEmpty()) {
                    Element argumentElement = newElement(connectionElement, "argument");
                    argumentValue.writeState(argumentElement);
                }

            }
        }
    }
}
