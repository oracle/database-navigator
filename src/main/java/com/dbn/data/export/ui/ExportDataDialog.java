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

package com.dbn.data.export.ui;

import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.export.DataExportInstructions;
import com.dbn.data.export.DataExportManager;
import com.dbn.data.export.DataExportSource;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;

public class ExportDataDialog extends DBNDialog<ExportDataForm> {
    private final DataExportSource source;
    private DataExportInstructions instructions;

    public ExportDataDialog(@NotNull DataExportSource source, @Nullable DataExportInstructions instructions) {
        super(source.getProject(), "Export data", true);
        this.source = source;
        this.instructions = instructions;
        init();
    }

    @NotNull
    @Override
    protected ExportDataForm createForm() {

        ResultSetTable<?> sourceTable = source.getTable();
        DBObject sourceObject = source.getObject();
        ConnectionHandler connection = source.getConnection();

        if (instructions == null) {
            DataExportManager exportManager = DataExportManager.getInstance(connection.getProject());
            instructions = exportManager.getExportInstructions().clone();
            instructions.setBaseName(sourceTable.getName());
        }

        boolean hasSelection = sourceTable.getSelectedRowCount() > 1 || sourceTable.getSelectedColumnCount() > 1;
        return new ExportDataForm(this, instructions, hasSelection, connection, sourceObject);
    }

    @NotNull
    @Override
    protected Action[] initializeActions() {
        renameAction(getOKAction(), "Export");
        return actions(
                getOKAction(),
                getCancelAction());
    }

    @Override
    protected void doOKAction() {
        getForm().validateEntries(
                () -> {
                    Project project = getProject();
                    DataExportInstructions instructions = getForm().getExportInstructions();

                    super.doOKAction();

                    ConnectionHandler connection = source.getConnection();
                    Progress.prompt(project, connection, true,
                            txt("prc.data.title.ExportingData"),
                            txt("prc.data.text.ExportingDataTo", instructions.getFormat(), instructions.getDestination()),
                            progress -> {
                                DataExportManager exportManager = DataExportManager.getInstance(project);
                                exportManager.exportTableContent(source, instructions);
                            });
                }
        );
    }
}
