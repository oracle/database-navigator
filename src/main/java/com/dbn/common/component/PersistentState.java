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

package com.dbn.common.component;

import com.dbn.common.thread.ThreadMonitor;
import com.dbn.common.thread.ThreadProperty;
import com.dbn.common.util.Unsafe;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PersistentState extends PersistentStateComponent<Element> {
    @Nullable
    default Project getProject() {
        return null;
    }

    @Override
    @Nullable
    default Element getState() {
        return ThreadMonitor.surround(
                getProject(),
                ThreadProperty.COMPONENT_STATE,
                () -> Unsafe.warned(null,
                        () -> getComponentState()));
    }

    @Override
    default void loadState(@NotNull Element state) {
        ThreadMonitor.surround(
                getProject(),
                ThreadProperty.COMPONENT_STATE,
                () -> Unsafe.warned(
                        () -> loadComponentState(state)));
    }

    @NonNls
    Element getComponentState();

    @NonNls
    void loadComponentState(@NotNull Element state);
}
