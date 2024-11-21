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

package com.dbn.execution.method;

import com.dbn.common.list.MostRecentStack;
import com.dbn.common.options.setting.Settings;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import lombok.Data;
import org.jdom.CDATA;
import org.jdom.Element;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Data
public class MethodExecutionArgumentValue implements PersistentStateElement, Cloneable<MethodExecutionArgumentValue>, ArgumentValueHolder<String> {
    private String name;
    private transient MostRecentStack<String> valueHistory = new MostRecentStack<>();

    public MethodExecutionArgumentValue(String name) {
        this.name = name;
    }

    public MethodExecutionArgumentValue(Element element) {
        readState(element);
    }

    public MethodExecutionArgumentValue(MethodExecutionArgumentValue source) {
        name = source.name;
        valueHistory.setValues(source.valueHistory.values());
    }

    public List<String> getValueHistory() {
        return valueHistory.values();
    }

    @Override
    public String getValue() {
        return valueHistory.get();
    }

    @Override
    public void setValue(String value) {
        valueHistory.stack(value);
    }

    @Override
    public void readState(Element element) {
        name = stringAttribute(element, "name");
        List<String> values = new ArrayList<>();
        String value = Commons.nullIfEmpty(element.getAttributeValue("value"));
        if (Strings.isNotEmpty(value)) {
            values.add(0, value);
        }

        for (Element child : element.getChildren()) {
            value = Settings.readCdata(child);
            if (Strings.isNotEmpty(value)) {
                values.add(value);
            }
        }
        valueHistory = new MostRecentStack<>(values);
    }

    @Override
    public void writeState(Element element) {
        element.setAttribute("name", name);
        for (String value : valueHistory) {
            Element valueElement = newElement(element, "value");

            CDATA cdata = new CDATA(value);
            valueElement.setContent(cdata);
        }
    }

    @Override
    public MethodExecutionArgumentValue clone() {
        return new MethodExecutionArgumentValue(this);
    }
}
