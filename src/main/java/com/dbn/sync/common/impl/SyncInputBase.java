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

package com.dbn.sync.common.impl;

import com.dbn.common.project.ProjectRef;
import com.dbn.sync.common.SyncElement;
import com.dbn.sync.common.SyncInput;
import com.intellij.openapi.project.Project;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.dbn.common.util.Lists.filter;

@Getter
public abstract class SyncInputBase<E extends SyncElement> implements SyncInput<E> {
    private final ProjectRef project;
    private final List<E> elements = new ArrayList<>();

    public SyncInputBase(Project project) {
        this.project = ProjectRef.of(project);
    }

    protected void addElement(E element) {
        elements.add(element);
    }

    protected void addElements(Collection<E> elements) {
        this.elements.addAll(elements);
    }

    public final List<E> getSelectedElements() {
        return filter(elements, e -> e.isSelected());
    }

    public final Project getProject() {
        return ProjectRef.ensure(project);
    }
}
