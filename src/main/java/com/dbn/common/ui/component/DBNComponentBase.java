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

package com.dbn.common.ui.component;

import com.dbn.common.action.Lookups;
import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.project.ProjectRef;
import com.dbn.common.project.ProjectSupplier;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;

public abstract class DBNComponentBase extends StatefulDisposableBase implements DBNComponent {
    private final ProjectRef project;
    private WeakRef<Disposable> parent;

    public DBNComponentBase(@Nullable Disposable parent) {
        this.parent = WeakRef.of(parent);
        this.project = null;

        registerDisposable(parent);
    }

    @Deprecated // load project from data context
    public DBNComponentBase(Disposable parent, @Nullable Project project) {
        this.parent = WeakRef.of(parent);
        this.project = ProjectRef.of(project);
        registerDisposable(parent);
    }

    public final void setParent(Disposable parent) {
        this.parent = WeakRef.of(parent);
        registerDisposable(parent);
    }

    private void registerDisposable(Disposable parent) {
        if (parent instanceof DBNDialog) {
            DBNDialog dialog = (DBNDialog) parent;
            Disposer.register(dialog.getDisposable(), this);
        } else {
            Disposer.register(parent, this);
        }
    }

    @Nullable
    @Override
    public final <T extends Disposable> T getParentComponent() {
        return (T) WeakRef.get(parent);
    }

    @Override
    @Nullable
    public final Project getProject() {
        if (project != null) {
            return project.ensure();
        }

        if (this.parent != null) {
            Disposable parent = this.parent.ensure();

            if (parent instanceof ProjectSupplier) {
                ProjectSupplier component = (ProjectSupplier) parent;
                Project project = component.getProject();
                if (project != null) {
                    return project;
                }
            }
        }

        return Lookups.getProject(getComponent());
    }
}
