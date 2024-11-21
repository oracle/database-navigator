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

package com.dbn.object.factory;

import com.dbn.common.component.Components;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.ref.WeakRefCache;
import com.dbn.language.common.psi.BasePsiElement;
import com.dbn.object.common.DBVirtualObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class VirtualObjectFactory extends ProjectComponentBase {

    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseObjectFactory";

    private final WeakRefCache<DBVirtualObject, Boolean> cache = WeakRefCache.weakKey();

    private VirtualObjectFactory(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static VirtualObjectFactory getInstance(@NotNull Project project) {
        return Components.projectService(project, VirtualObjectFactory.class);
    }


    public DBVirtualObject createVirtualObject(BasePsiElement<?> psiElement) {
        DBVirtualObject virtualObject = new DBVirtualObject(psiElement);
        cache.set(virtualObject, true);
        return virtualObject;
    }

    @Override
    public void disposeInner() {
        Disposer.dispose(cache.keys());
        super.disposeInner();
    }
}