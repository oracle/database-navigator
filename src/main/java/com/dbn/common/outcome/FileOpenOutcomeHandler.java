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

package com.dbn.common.outcome;

import com.dbn.common.Priority;
import com.dbn.common.project.ProjectRef;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;

import java.util.List;
import java.util.function.Supplier;

public class FileOpenOutcomeHandler implements OutcomeHandler {
    private final ProjectRef project;
    private final Supplier<List<VirtualFile>> filesSupplier;

    private FileOpenOutcomeHandler(Project project, Supplier<List<VirtualFile>> filesSupplier) {
        this.project = ProjectRef.of(project);
        this.filesSupplier = filesSupplier;
    }

    public static FileOpenOutcomeHandler create(Project project, Supplier<List<VirtualFile>> filesSupplier) {
        return new FileOpenOutcomeHandler(project, filesSupplier);
    }

    @Override
    public void handle(Outcome outcome) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project.ensure());
        for (VirtualFile generatedFile : filesSupplier.get()) {
            fileEditorManager.openFile(generatedFile, true);
        }
    }

    @Override
    public Priority getPriority() {
        return Priority.LOW;
    }
}
