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

package com.dbn.common.action;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.util.Context;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.Component;

@UtilityClass
public class Lookups {

    @Nullable
    public static Project getProject(AnActionEvent e) {
        return e.getData(PlatformDataKeys.PROJECT);
    }

    @NotNull
    public static Project ensureProject(AnActionEvent e) {
        return Failsafe.nn(e.getData(PlatformDataKeys.PROJECT));
    }

    @Nullable
    public static VirtualFile getVirtualFile(@Nullable AnActionEvent e) {
        if (e == null) return null;
        return e.getData(PlatformDataKeys.VIRTUAL_FILE);
    }

    @Nullable
    public static VirtualFile getVirtualFile(@NotNull Component component) {
        DataContext dataContext = Context.getDataContext(component);
        return getVirtualFile(dataContext);
    }

    @Nullable
    public static VirtualFile getVirtualFile(@NotNull DataContext dataContext) {
        return PlatformDataKeys.VIRTUAL_FILE.getData(dataContext);
    }

    @Nullable
    public static Editor getEditor(@NotNull AnActionEvent e) {
        return e.getData(PlatformDataKeys.EDITOR);
    }

    @Nullable
    public static FileEditor getFileEditor(@NotNull AnActionEvent e) {
        return e.getData(PlatformDataKeys.FILE_EDITOR);
    }

    @Nullable
    public static FileEditor getFileEditor(@NotNull DataContext dataContext) {
        return PlatformDataKeys.FILE_EDITOR.getData(dataContext);
    }

    public static Project getProject(Component component){
        DataContext dataContext = Context.getDataContext(component);
        return PlatformDataKeys.PROJECT.getData(dataContext);
    }
}
