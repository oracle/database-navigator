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

package com.dbn.connection.config.tns.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.ui.dialog.OptionsDialog;
import com.dbn.connection.config.tns.TnsImportData;
import com.dbn.connection.config.tns.TnsImportService;
import com.dbn.connection.config.tns.TnsImportType;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;
import java.io.File;

@Getter
public class TnsNamesImportDialog extends DBNDialog<TnsNamesImportForm> {
    private final TnsImportData importData = new TnsImportData();

    private final AbstractAction importAllAction = new ImportAllAction();
    private final AbstractAction importSelectedAction = new ImportSelectedAction();
    private final File file;

    public TnsNamesImportDialog(Project project, @Nullable File file) {
        super(project, "Import TNS names", true);
        this.file = file;
        setModal(true);
        init();
    }

    @NotNull
    @Override
    protected TnsNamesImportForm createForm() {
        return new TnsNamesImportForm(this, file);
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        return new Action[]{
                importSelectedAction,
                importAllAction,
                getCancelAction(),
        };
    }

    private class ImportAllAction extends AbstractAction {
        private ImportAllAction() {
            super("Import All");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TnsNamesImportForm importForm = getForm();
            importData.setTnsNames(importForm.getTnsNames());
            importData.setSelectedOnly(false);
            showImportTypeDialog();
        }
    }
    
    private class ImportSelectedAction extends AbstractAction {
        private ImportSelectedAction() {
            super("Import Selected");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            TnsNamesImportForm importForm = getForm();
            importData.setTnsNames(importForm.getTnsNames());
            importData.setSelectedOnly(true);
            showImportTypeDialog();
        }
    }

    private void showImportTypeDialog() {
        TnsImportService importService = TnsImportService.getInstance();
        OptionsDialog.open(
                getProject(),
                "TNS Import Type",
                "Import Type",
                TnsImportType.values(),
                importService.getImportType(),
                new String[]{"Import"},
                (i, o) -> {
                    if (i != 0) return;
                    importData.setImportType(o);
                    importService.setImportType(o);
                    doOKAction();
            });

/*
        Messages.showQuestionDialog(
                getProject(),
                "TNS Import Type",
                "What type of TNS import to perform?",
                options("TNS Fields",
                        "TNS Profile Link",
                        "Full TNS Description",
                        "Cancel"), 0,
                option -> {
                    switch (option) {
                        case 0: importType = TnsImportType.FIELDS; doOKAction(); break;
                        case 1: importType = TnsImportType.PROFILE; doOKAction(); break;
                        case 2: importType = TnsImportType.DESCRIPTOR; doOKAction(); break;
                        default: importType = null;
                    }
                });
*/
    }
}
