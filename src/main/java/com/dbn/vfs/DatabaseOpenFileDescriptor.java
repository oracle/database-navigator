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

package com.dbn.vfs;

import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public class DatabaseOpenFileDescriptor extends OpenFileDescriptor {
    public DatabaseOpenFileDescriptor(Project project, @NotNull VirtualFile file, int offset) {
        super(project, file, offset);
    }

    public DatabaseOpenFileDescriptor(Project project, @NotNull VirtualFile file, int line, int col) {
        super(project, file, line, col);
    }

    public DatabaseOpenFileDescriptor(Project project, @NotNull VirtualFile file) {
        super(project, file);
    }

    @Override
    public void navigate(boolean requestFocus) {
        super.navigate(requestFocus);
    }

    @Override
    public void navigateIn(@NotNull Editor e) {
        super.navigateIn(e);
    }

    @Override
    public boolean canNavigate() {
        return super.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
        return super.canNavigateToSource();
    }

    @Override
    public boolean navigateInEditor(@NotNull Project project, boolean requestFocus) {
        return super.navigateInEditor(project, requestFocus);
    }

/*
    @Override
    protected boolean navigateInAnyFileEditor(Project project, boolean focusEditor) {
        return super.navigateInAnyFileEditor(project, focusEditor);
    }
*/

    @NotNull
    @Override
    public VirtualFile getFile() {
        VirtualFile file = super.getFile();
        if (file instanceof DBSourceCodeVirtualFile) {
            DBSourceCodeVirtualFile sourceCodeFile = (DBSourceCodeVirtualFile) file;
            return sourceCodeFile.getMainDatabaseFile();
        }

        return file;
    }
}
