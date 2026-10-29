/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.common.component;

import com.dbn.common.constant.Constant;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.state.StateAttributes;
import com.dbn.common.state.StateCategory;
import com.dbn.common.state.StateContainer;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.newStateElement;

public abstract class ServiceComponentBase extends StatefulDisposableBase implements PersistentStateElement, Service {
    private final String componentName;
    private volatile StateContainer userInterfaceState;

    protected ServiceComponentBase(String componentName) {
        this.componentName = componentName;
    }

    @NotNull
    public final String getComponentName() {
        return componentName;
    }

    @NotNull
    public final StateAttributes getUserInterfaceState(@NonNls String category) {
        return ensureUserInterfaceState().ensureAttributes(StateCategory.get(category));
    }

    @NotNull
    public final StateAttributes getUserInterfaceState(@NotNull Constant<?> category) {
        return ensureUserInterfaceState().ensureAttributes(StateCategory.get(category.id()));
    }

    @NonNls
    @NotNull
    public final Element getComponentState() {
        Element element = newStateElement();
        writeState(element);
        return element;
    }

    public final void loadComponentState(@NotNull @NonNls Element element) {
        readState(element);
    }

    @Override
    public void readState(Element element) {
        Element stateElement = element.getChild("user-interface-state");
        if (stateElement == null) return;

        ensureUserInterfaceState().readState(stateElement);
    }

    @Override
    public void writeState(Element element) {
        StateContainer state = userInterfaceState;
        if (state == null) return;

        state.writeState(element, "user-interface-state");
    }

    @NotNull
    private StateContainer ensureUserInterfaceState() {
        StateContainer state = userInterfaceState;
        if (state != null) return state;

        synchronized (this) {
            state = userInterfaceState;
            if (state == null) {
                state = new StateContainer();
                userInterfaceState = state;
            }
        }
        return state;
    }
}
