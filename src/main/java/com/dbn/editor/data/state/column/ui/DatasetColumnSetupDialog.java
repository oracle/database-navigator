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

package com.dbn.editor.data.state.column.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.editor.data.DatasetEditor;
import com.dbn.editor.data.DatasetLoadInstructions;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

import static com.dbn.editor.data.DatasetLoadInstruction.DELIBERATE_ACTION;
import static com.dbn.editor.data.DatasetLoadInstruction.PRESERVE_CHANGES;
import static com.dbn.editor.data.DatasetLoadInstruction.REBUILD;
import static com.dbn.editor.data.DatasetLoadInstruction.USE_CURRENT_FILTER;

public class DatasetColumnSetupDialog extends DBNDialog<DatasetColumnSetupForm> {
    private static final DatasetLoadInstructions LOAD_INSTRUCTIONS = new DatasetLoadInstructions(USE_CURRENT_FILTER, PRESERVE_CHANGES, DELIBERATE_ACTION, REBUILD);
    private DatasetEditor datasetEditor;

    public DatasetColumnSetupDialog(@NotNull DatasetEditor datasetEditor) {
        super(datasetEditor.getProject(), "Column setup", true);
        this.datasetEditor = datasetEditor;
        setModal(true);
        setResizable(true);
        renameAction(getCancelAction(), "Cancel");
        init();
    }

    @NotNull
    @Override
    protected DatasetColumnSetupForm createForm() {
        return new DatasetColumnSetupForm(this, datasetEditor);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                getOKAction(),
                getCancelAction(),
                getHelpAction()
        };
    }

    @Override
    protected void doOKAction() {
        boolean changed = getForm().applyChanges();
        if (changed) {
            datasetEditor.loadData(LOAD_INSTRUCTIONS);
        }
        super.doOKAction();
    }

    @Override
    public void disposeInner() {
        datasetEditor = null;
    }
}
