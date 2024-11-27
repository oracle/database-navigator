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

package com.dbn.editor.data.record.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.editor.data.model.DatasetEditorModelRow;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class DatasetRecordEditorDialog extends DBNDialog<DatasetRecordEditorForm> {
    private final DatasetEditorModelRow row;
    public DatasetRecordEditorDialog(Project project, DatasetEditorModelRow row) {
        super(project, row.getModel().isEditable() ? "Edit record" : "View record", true);
        this.row = row;
        setModal(true);
        setResizable(true);
        renameAction(getCancelAction(), "Close");
        init();
    }

    @NotNull
    @Override
    protected DatasetRecordEditorForm createForm() {
        return new DatasetRecordEditorForm(this, row);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getCancelAction(),
                getHelpAction()
        };
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
    }
}
