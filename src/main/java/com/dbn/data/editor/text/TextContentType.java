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

package com.dbn.data.editor.text;

import com.dbn.common.ui.list.Selectable;
import com.dbn.editor.data.options.DataEditorQualifiedEditorSettings;
import com.dbn.editor.data.options.DataEditorSettings;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.Objects;

@Data
public class TextContentType implements Selectable<TextContentType> {
    private final String name;
    private final FileType fileType;
    private transient boolean selected = true;

    public TextContentType(String name, FileType fileType) {
        this.name = name.intern();
        this.fileType = fileType;
    }

    @Nullable
    public static TextContentType create(String name, String fileTypeName) {
        FileType fileType = FileTypeManager.getInstance().getStdFileType(fileTypeName);
        // if returned expected file type
        if (Objects.equals(fileType.getName(), fileTypeName)) {
            return new TextContentType(name, fileType);
        }
        return null;
    }

    public static TextContentType get(Project project, String contentTypeName) {
        DataEditorQualifiedEditorSettings qualifiedEditorSettings = DataEditorSettings.getInstance(project).getQualifiedEditorSettings();
        TextContentType contentType = qualifiedEditorSettings.getContentType(contentTypeName);
        return contentType == null ? getPlainText(project) : contentType;
    }

    public static TextContentType getPlainText(Project project) {
        return get(project, "Text");
    }

    @Override
    public String getError() {
        return null;
    }

    @Override
    public Icon getIcon() {
        return fileType.getIcon();
    }

    @Override
    public boolean isMasterSelected() {
        return true;
    }

    @Override
    public int compareTo(@NotNull TextContentType remote) {
        return name.compareTo(remote.name);
    }
}

