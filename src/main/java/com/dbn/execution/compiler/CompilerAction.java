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

package com.dbn.execution.compiler;

import com.dbn.common.ref.WeakRef;
import com.dbn.common.util.Editors;
import com.dbn.editor.DBContentType;
import com.dbn.editor.EditorProviderId;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

@Getter
@Setter
public class CompilerAction {
    private final CompilerActionSource source;
    private final DBContentType contentType;
    private WeakRef<VirtualFile> virtualFile;
    private WeakRef<FileEditor> fileEditor;
    private EditorProviderId editorProviderId;
    private int sourceStartOffset;

    public CompilerAction(CompilerActionSource source, DBContentType contentType) {
        this.source = source;
        this.contentType = contentType;
    }

    public CompilerAction(CompilerActionSource source, DBContentType contentType, @Nullable VirtualFile virtualFile, @Nullable FileEditor fileEditor) {
        this.source = source;
        this.contentType = contentType;
        this.editorProviderId = contentType.getEditorProviderId();
        setVirtualFile(virtualFile);
        setFileEditor(fileEditor);
    }

    public void setVirtualFile(VirtualFile virtualFile) {
        this.virtualFile = WeakRef.of(virtualFile);
    }

    public void setFileEditor(FileEditor fileEditor) {
        this.fileEditor = WeakRef.of(fileEditor);
    }

    public boolean isDDL() {
        return source == CompilerActionSource.DDL;
    }

    public boolean isSave() {
        return source == CompilerActionSource.SAVE;
    }

    public boolean isCompile() {
        return source == CompilerActionSource.COMPILE;
    }

    public boolean isBulkCompile() {
        return source == CompilerActionSource.BULK_COMPILE;
    }

    @Nullable
    public VirtualFile getVirtualFile() {
        return WeakRef.get(virtualFile);
    }

    @Nullable
    public FileEditor getFileEditor() {
        FileEditor fileEditor = WeakRef.get(this.fileEditor);
        if (fileEditor != null) {
            Editor editor = Editors.getEditor(fileEditor);
            if (editor != null) {
                // TODO why?
                this.fileEditor = null;
            }
        }
        return fileEditor;
    }
}
