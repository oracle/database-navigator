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

import com.dbn.common.list.MostRecentStack;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Strings;
import com.dbn.execution.ExecutionInputMode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.readCdata;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.options.setting.Settings.writeCdata;

@Getter
@Setter
@NoArgsConstructor
public class ExecutionVariable implements PersistentStateElement, Cloneable<ExecutionVariable>, ValueHolder<String> {
    private String path;
    private ExecutionInputMode mode = ExecutionInputMode.FIELDS;
    private MostRecentStack<String> valueHistory = new MostRecentStack<>();
    private MostRecentStack<String> expressionHistory = new MostRecentStack<>();


    public ExecutionVariable(String path) {
        this.path = path;
    }

    public ExecutionVariable(ExecutionVariable source) {
        this.path = source.path;
        this.mode = source.mode;
        this.valueHistory.setValues(source.valueHistory.values());
        this.expressionHistory.setValues(source.expressionHistory.values());
    }

    public List<String> getValueHistory() {
        return valueHistory.values();
    }

    public List<String> getExpressionHistory() {
        return expressionHistory.values();
    }

    public String getValue() {
        return getContainer().get();
    }

    public void setValue(String value) {
        if (value == null) return;

        MostRecentStack<String> container = getContainer();
        container.stack(value);
    }
    
    public String getValue(ExecutionInputMode mode) {
        return getContainer(mode).get();
    }

    private MostRecentStack<String> getContainer() {
        return getContainer(mode);
    }

    private MostRecentStack<String> getContainer(ExecutionInputMode mode) {
        return mode == ExecutionInputMode.CODE ?
                expressionHistory :
                valueHistory;
    }


    @Override
    public void readState(Element element) {
        path = stringAttribute(element, "path");
        mode = enumAttribute(element, "mode", mode);
        List<String> values = new ArrayList<>();
        List<String> expressions = new ArrayList<>();

        for (Element valueElement : element.getChildren("value")) {
            String value = readCdata(valueElement);
            if (Strings.isNotEmpty(value)) {
                values.add(value);
            }
        }

        for (Element exprElement : element.getChildren("expression")) {
            String expr = readCdata(exprElement);
            if (Strings.isNotEmpty(expr)) {
                expressions.add(expr);
            }
        }
        valueHistory = new MostRecentStack<>(values);
        expressionHistory = new MostRecentStack<>(expressions);

    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "path", path);
        setEnumAttribute(element, "mode", mode);
        for (String value : valueHistory) {
            Element valueElement = newElement(element, "value");
            writeCdata(valueElement, value, true);
        }
        for (String expr : expressionHistory) {
            Element exprElement = newElement(element, "expression");
            writeCdata(exprElement, expr, true);
        }
    }

    @Override
    public ExecutionVariable clone() {
        return new ExecutionVariable(this);
    }
}
