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

package com.dbn.editor.ddl;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.editor.BasicTextEditor;
import com.dbn.common.editor.BasicTextEditorProvider;
import com.dbn.common.file.util.VirtualFiles;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Getter
public abstract class DDLFileEditorProvider extends BasicTextEditorProvider implements DumbAware {

    private final int index;
    private final String componentName;

    public DDLFileEditorProvider(int index, String componentName) {
        this.index = index;
        this.componentName = componentName;
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) virtualFile;
            List<VirtualFile> ddlFiles = databaseFile.getAttachedDDLFiles();
            return ddlFiles != null && ddlFiles.size() > index;
        }
        return false;
    }

    @Override
    @NotNull
    public FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile file) {
        DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) file;
        List<VirtualFile> ddlFiles = Failsafe.nn(databaseFile.getAttachedDDLFiles());
        VirtualFile virtualFile = ddlFiles.get(index);

        BasicTextEditor textEditor = new DDLFileEditor(project, virtualFile, getEditorProviderId());
        updateTabIcon(databaseFile, textEditor, VirtualFiles.getIcon(virtualFile));
        return textEditor;
    }

    @Override
    public void disposeEditor(@NotNull FileEditor editor) {
        DDLFileEditor sourceEditor = (DDLFileEditor) editor;
        Document document = sourceEditor.getEditor().getDocument();
        //DocumentUtil.removeGuardedBlock(document);
        Disposer.dispose(sourceEditor);
    }

    @Override
    @NotNull
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;

    }

    public String getName() {
        return null;
    }

    /*********************************************************
     *                ApplicationComponent                   *
     *********************************************************/

    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return componentName;
    }
}
