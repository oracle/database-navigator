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
import com.dbn.common.state.ProtectedContent;
import com.dbn.common.state.ProtectedContents;
import com.dbn.common.util.Cloneable;
import com.dbn.execution.ExecutionInputMode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdom.Element;

import java.util.List;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
@NoArgsConstructor
public class ExecutionVariable implements PersistentStateElement, Cloneable<ExecutionVariable>, ValueHolder<String> {
    private String path;
    private ExecutionInputMode mode = ExecutionInputMode.FIELDS;
    private final ProtectedContents valueHistory = ProtectedContents.executionVariableValues();
    private final ProtectedContents expressionHistory = ProtectedContents.executionVariableExpressions();


    public ExecutionVariable(String path) {
        this.path = path;
    }

    public ExecutionVariable(ExecutionVariable source) {
        this.path = source.path;
        this.mode = source.mode;
        valueHistory.copyFrom(source.valueHistory);
        expressionHistory.copyFrom(source.expressionHistory);
    }

    public List<String> getValueHistory() {
        return valueHistory.values();
    }

    public List<String> getExpressionHistory() {
        return expressionHistory.values();
    }

    public String getValue() {
        return getContainer().getValue();
    }

    public void setValue(String value) {
        if (value == null) return;

        getContainer().setValue(value);
    }
    
    public String getValue(ExecutionInputMode mode) {
        return getContainer(mode).getValue();
    }

    private ProtectedContents getContainer() {
        return getContainer(mode);
    }

    private ProtectedContents getContainer(ExecutionInputMode mode) {
        return mode == ExecutionInputMode.CODE ?
                expressionHistory :
                valueHistory;
    }


    @Override
    public void readState(Element element) {
        path = stringAttribute(element, "path");
        mode = enumAttribute(element, "mode", mode);

        valueHistory.clear();
        for (Element valueElement : element.getChildren("value")) {
            ProtectedContent value = valueHistory.newContent();
            value.readState(valueElement);
            valueHistory.add(value);
        }

        expressionHistory.clear();
        for (Element exprElement : element.getChildren("expression")) {
            ProtectedContent expression = expressionHistory.newContent();
            expression.readState(exprElement);
            expressionHistory.add(expression);
        }
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "path", path);
        setEnumAttribute(element, "mode", mode);
        for (ProtectedContent value : valueHistory) {
            if (value.isEmpty()) continue;

            Element valueElement = newElement(element, "value");
            value.writeState(valueElement);
        }
        for (ProtectedContent expression : expressionHistory) {
            if (expression.isEmpty()) continue;

            Element exprElement = newElement(element, "expression");
            expression.writeState(exprElement);
        }
    }

    @Override
    public ExecutionVariable clone() {
        return new ExecutionVariable(this);
    }
}
