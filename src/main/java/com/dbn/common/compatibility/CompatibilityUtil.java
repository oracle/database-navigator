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

package com.dbn.common.compatibility;

import com.intellij.find.editorHeaderActions.Utils;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.UIManager;

public class CompatibilityUtil {
    public static void setSmallerFont(JComponent component) {
        Utils.setSmallerFont(component);
    }

    public static boolean isUnderGTKLookAndFeel() {
        return SystemInfo.isXWindow && UIManager.getLookAndFeel().getName().contains("GTK");
    }

    @Nullable
    public static FileEditor getSelectedEditor(Project project) {
        FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
        //return fileEditorManager.getSelectedEditor();

        VirtualFile[] files = fileEditorManager.getSelectedFiles();
        return files.length == 0 ? null : fileEditorManager.getSelectedEditor(files[0]);
    }
}
