/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.state;

import org.jdom.Element;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.constantAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setConstantAttribute;

public class StateContainer implements PersistentStateElement{
    private final Map<StateCategory, StateAttributes> attributes = new ConcurrentHashMap<>();

    @Nullable
    public StateAttributes getAttributes(StateCategory category) {
        return attributes.get(category);
    }

    public StateAttributes ensureAttributes(StateCategory category) {
        return attributes.computeIfAbsent(category, c -> new StateAttributes());
    }

    @Override
    public void readState(Element element) {
        for (Element stateElement : childrenOf(element, "state")) {
            StateCategory category = constantAttribute(stateElement, "category", StateCategory.class);
            StateAttributes state = new StateAttributes();
            state.readState(stateElement);
            attributes.put(category, state);
        }
    }

    @Override
    public void writeState(Element element) {
        for (StateCategory category : attributes.keySet()) {
            Element stateElement = newElement(element, "state");
            setConstantAttribute(stateElement, "category", category);

            StateAttributes state = attributes.get(category);
            state.writeState(stateElement);
        }
    }
}
