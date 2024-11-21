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

package com.dbn.editor.code;

import com.dbn.editor.DBContentType;
import com.dbn.editor.EditorProviderId;
import com.dbn.vfs.file.DBEditableObjectVirtualFile;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class SourceCodeSpecEditorProvider extends SourceCodeEditorProviderBase {

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        DBEditableObjectVirtualFile databaseFile = null;
        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            databaseFile = (DBEditableObjectVirtualFile) virtualFile;
            DBContentType contentType = databaseFile.getContentType();
            return contentType == DBContentType.CODE_SPEC_AND_BODY;
        }

        return super.accept(project, virtualFile);
    }

    @Override
    public DBContentType getContentType() {
        return DBContentType.CODE_SPEC;
    }

    @Override
    @NotNull
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR;
    }

    @NotNull
    @Override
    public EditorProviderId getEditorProviderId() {
        return EditorProviderId.CODE_SPEC;
    }

    @Override
    public String getName() {
        return "Spec";
    }

    @Override
    public Icon getIcon() {
        return null;//Icons.CODE_EDITOR_SPEC;
    }

    /*********************************************************
     *                ApplicationComponent                   *
     *********************************************************/

    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.DBSourceSpecEditorProvider";
    }

}
