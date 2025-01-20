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

package com.dbn.generator.code;

import com.dbn.common.options.setting.Settings;
import com.dbn.common.state.PersistentStateElement;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setStringAttribute;

/**
 * Generic state holder for the code generator.
 * Allows storing an indefinite number of properties to be persisted and reused whenever the code generator is invoked again.
 * e.g. can be used for remembering and restoring the module and content-root selection for the generated files.
 *
 * @author Dan Cioca (Oracle)
 */
public class CodeGeneratorState implements PersistentStateElement {
    private final Map<String, String> properties = new HashMap<>();

    public String getAttribute(@NonNls String key) {
        return properties.get(key);
    }

    public void setAttribute(@NonNls String key, @NonNls String value) {
        properties.put(key, value);
    }

    @Override
    public void readState(Element element) {
        List<Element> children = element.getChildren();
        for (Element child : children) {
            String key = child.getName();
            String value = Settings.stringAttribute(child, "value");
            properties.put(key, value);
        }
    }

    @Override
    public void writeState(Element element) {
        properties.forEach((key, value) -> {
            Element child = newElement(element, key);
            setStringAttribute(child, "value", value);
        });
    }
}
