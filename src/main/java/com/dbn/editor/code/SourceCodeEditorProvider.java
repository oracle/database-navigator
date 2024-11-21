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

import static com.dbn.database.DatabaseFeature.OBJECT_SOURCE_EDITING;
import static com.dbn.editor.DBContentType.CODE;
import static com.dbn.editor.DBContentType.CODE_AND_DATA;

public class SourceCodeEditorProvider extends SourceCodeEditorProviderBase {

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        if (virtualFile instanceof DBEditableObjectVirtualFile) {
            DBEditableObjectVirtualFile databaseFile = (DBEditableObjectVirtualFile) virtualFile;

            DBContentType contentType = databaseFile.getContentType();
            return contentType.isOneOf(CODE, CODE_AND_DATA) &&
                    OBJECT_SOURCE_EDITING.isSupported(databaseFile);

        }
        return super.accept(project, virtualFile);
    }


    @Override
    public DBContentType getContentType() {
        return CODE;
    }

    @Override
    @NotNull
    public FileEditorPolicy getPolicy() {
        return FileEditorPolicy.HIDE_DEFAULT_EDITOR;

    }

    @NotNull
    @Override
    public EditorProviderId getEditorProviderId() {
        return EditorProviderId.CODE;
    }

    @Override
    public String getName() {
        return "Code";
    }

    @Override
    public Icon getIcon() {
        return null;
    }

    /*********************************************************
     *                ApplicationComponent                   *
     *********************************************************/

    @Override
    @NonNls
    @NotNull
    public String getComponentName() {
        return "DBNavigator.DBSourceEditorProvider";
    }

}
