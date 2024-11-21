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

package com.dbn.diagnostics.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.common.thread.Progress;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.diagnostics.ParserDiagnosticsManager;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.io.File;

@Slf4j
public class ExportScrambledSourcecodeAction extends ProjectAction {
    public static final FileChooserDescriptor FILE_CHOOSER_DESCRIPTOR = new FileChooserDescriptor(false, true, false, false, false, false).
            withTitle("Select Destination Directory").
            withDescription("Select destination directory for the scrambled sources");

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        VirtualFile[] virtualFiles = FileChooser.chooseFiles(FILE_CHOOSER_DESCRIPTOR, project, null);
        if (virtualFiles.length == 1) {
            Progress.modal(project, null, true,
                    "Scrambling code",
                    "Running project code scrambler",
                    progress -> {
                        progress.setIndeterminate(false);
                        ParserDiagnosticsManager manager = ParserDiagnosticsManager.get(project);
                        manager.scrambleProjectFiles(progress, new File(virtualFiles[0].getPath()));
                    });
        }
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Presentation presentation = e.getPresentation();
        presentation.setVisible(Diagnostics.isBulkActionsEnabled());
        presentation.setText("Export Scrambled Sourcecode");
    }


}
