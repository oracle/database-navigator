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

package com.dbn.editor.code.action;

import com.dbn.editor.code.diff.SourceCodeDiffManager;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

abstract class AbstractCodeEditorDiffAction extends AbstractCodeEditorAction {
    public AbstractCodeEditorDiffAction() {
    }

    void openDiffWindow(
            @NotNull Project project,
            @NotNull DBSourceCodeVirtualFile sourceCodeFile,
            String referenceText,
            String referenceTitle,
            String windowTitle) {
        SourceCodeDiffManager diffManager = SourceCodeDiffManager.getInstance(project);
        diffManager.openDiffWindow(sourceCodeFile, referenceText, referenceTitle, windowTitle);
    }
}

