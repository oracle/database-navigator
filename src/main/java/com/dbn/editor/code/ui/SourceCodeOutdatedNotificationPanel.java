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

package com.dbn.editor.code.ui;

import com.dbn.common.message.MessageType;
import com.dbn.database.DatabaseFeature;
import com.dbn.editor.code.SourceCodeEditor;
import com.dbn.editor.code.SourceCodeManager;
import com.dbn.editor.code.diff.MergeAction;
import com.dbn.editor.code.diff.SourceCodeDiffManager;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.vfs.file.DBSourceCodeVirtualFile;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.util.text.DateFormatUtil;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.common.util.Strings.toLowerCase;

public class SourceCodeOutdatedNotificationPanel extends SourceCodeEditorNotificationPanel{
    public SourceCodeOutdatedNotificationPanel(DBSourceCodeVirtualFile sourceCodeFile, @NotNull FileEditor fileEditor, SourceCodeEditor sourceCodeEditor) {
        super(sourceCodeFile.getObject(), fileEditor, MessageType.WARNING);
        DBSchemaObject editableObject = sourceCodeFile.getObject();
        Project project = editableObject.getProject();
        String presentableChangeTime =
                DatabaseFeature.OBJECT_CHANGE_MONITORING.isSupported(editableObject) ?
                        toLowerCase(DateFormatUtil.formatPrettyDateTime(sourceCodeFile.getDatabaseChangeTimestamp())) : "";


        String text = "Outdated version";
        boolean mergeRequired = sourceCodeFile.isMergeRequired();
        if (sourceCodeFile.isModified() && !mergeRequired) {
            text += " (MERGED)";
        }
        text += ". The " + editableObject.getQualifiedNameWithType() + " was changed in database by another user (" + presentableChangeTime + ")";

        setText(text);
        createActionLabel("Show diff", () -> {
            if (isNotValid(project)) return;

            SourceCodeDiffManager diffManager = SourceCodeDiffManager.getInstance(project);
            diffManager.opedDatabaseDiffWindow(sourceCodeFile);
        });

        if (mergeRequired) {
            createActionLabel("Merge", () -> {
                if (isNotValid(project)) return;

                SourceCodeDiffManager diffManager = SourceCodeDiffManager.getInstance(project);
                diffManager.openCodeMergeDialog(sourceCodeFile, sourceCodeEditor, MergeAction.MERGE);
            });
        }

        createActionLabel(sourceCodeFile.isModified() ? "Revert local changes" : "Reload", () -> {
            if (isNotValid(project)) return;

            SourceCodeManager sourceCodeManager = SourceCodeManager.getInstance(project);
            sourceCodeManager.loadSourceCode(sourceCodeFile, true);
        });
    }
}
