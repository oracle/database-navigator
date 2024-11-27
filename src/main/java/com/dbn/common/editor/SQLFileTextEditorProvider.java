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

package com.dbn.common.editor;

import com.dbn.common.file.VirtualFileDelegate;
import com.dbn.editor.EditorProviderId;
import com.dbn.language.common.DBLanguageFileType;
import com.dbn.language.sql.SQLFileType;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorPolicy;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.editor.EditorProviderId.DBN_SQL;
import static com.intellij.openapi.fileEditor.FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR;

public class SQLFileTextEditorProvider extends BasicTextEditorProvider{
    @NotNull
    @Override
    public EditorProviderId getEditorProviderId() {
        return DBN_SQL;
    }

    @Override
    public boolean accept(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        String extension = virtualFile.getExtension();

        FileTypeManager fileTypeManager = FileTypeManager.getInstance();
        FileType fileType = fileTypeManager.getFileTypeByFile(virtualFile);
        if (fileType instanceof DBLanguageFileType) return false;

        if (SQLFileType.INSTANCE.isSupported(virtualFile.getExtension())) {
            return true;
        }
        return false;
    }

    @Override
    public @NotNull FileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
        VirtualFile virtualFileDelegate = new VirtualFileDelegate(virtualFile, SQLFileType.INSTANCE);
        return new BasicTextEditorImpl(project, virtualFileDelegate, "DBN SQL", getEditorProviderId()){};
    }

    @Override
    public @NotNull FileEditorPolicy getPolicy() {
        return PLACE_AFTER_DEFAULT_EDITOR;
    }


    @NotNull
    @Override
    public String getComponentName() {
        return "DBNavigator.SQLFileTextEditorProvider";
    }
}
