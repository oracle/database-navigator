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

package com.dbn.object.action;

import com.dbn.common.action.ContextAction;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AnObjectAction<T extends DBObject> extends ContextAction<T>  {
    private final DBObjectRef<T> object;

    public AnObjectAction(@NotNull T object) {
        this.object = DBObjectRef.of(object);
    }

    @Override
    protected T getContext(@NotNull AnActionEvent e) {
        return getTarget();
    }

    public T getTarget() {
        return DBObjectRef.get(object);
    }

    @NotNull
    @Override
    public  Project getProject() {
        return object.ensure().getProject();
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Presentation presentation, @NotNull Project project, @Nullable T target) {
        if (target == null) return;
        presentation.setText(target.getPresentableText(), false);
        presentation.setIcon(target.getIcon());
    }
}
