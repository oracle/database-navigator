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

package com.dbn.data.record.ui;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.data.record.DatasetRecord;
import com.dbn.editor.data.DatasetEditorManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

public class RecordViewerDialog extends DBNDialog<RecordViewerForm> {
    private final DatasetRecord record;

    public RecordViewerDialog(Project project, DatasetRecord record) {
        super(project, "View record", true);
        this.record = record; 
        setModal(false);
        setResizable(true);
        renameAction(getCancelAction(), "Close");
        init();
    }

    @NotNull
    @Override
    protected RecordViewerForm createForm() {
        return new RecordViewerForm(this, record);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                new OpenInEditorAction(),
                getCancelAction(),
                getHelpAction()
        };
    }
    
    @Override
    protected void doOKAction() {
        super.doOKAction();
    }

    private class OpenInEditorAction extends AbstractAction {
        public OpenInEditorAction() {
            super("Open In Editor", Icons.OBJECT_EDIT_DATA);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DatasetEditorManager datasetEditorManager = DatasetEditorManager.getInstance(record.getDataset().getProject());
            datasetEditorManager.openDataEditor(record.getFilterInput());
            doCancelAction();
        }
    }
}
