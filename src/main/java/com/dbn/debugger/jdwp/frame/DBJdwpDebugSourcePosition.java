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

package com.dbn.debugger.jdwp.frame;

import com.dbn.object.common.DBObjectPsiCache;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.debugger.SourcePosition;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class DBJdwpDebugSourcePosition extends SourcePosition {
    private final PsiFile file;
    private final int line;

    public DBJdwpDebugSourcePosition(PsiFile file, int line) {
        this.file = file;
        this.line = line;
    }

    @NotNull
    public PsiFile getFile() {
        VirtualFile virtualFile = file.getVirtualFile();
        if (virtualFile instanceof DBSourceCodeVirtualFile) {
            DBSourceCodeVirtualFile sourceCodeVirtualFile = (DBSourceCodeVirtualFile) virtualFile;
            DBSchemaObject object = sourceCodeVirtualFile.getObject();
            return DBObjectPsiCache.asPsiFile(object);
        }
        return file;
    }

    @Override
    public PsiElement getElementAt() {
        return null;
    }

    @Override
    public int getOffset() {
        return 0;
    }

    @Override
    public Editor openEditor(boolean requestFocus) {
        return null;
    }

    @Override
    public void navigate(boolean requestFocus) {
    }

    @Override
    public boolean canNavigate() {
        return false;
    }

    @Override
    public boolean canNavigateToSource() {
        return false;
    }
}
