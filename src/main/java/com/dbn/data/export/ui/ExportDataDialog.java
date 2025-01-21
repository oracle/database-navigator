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

import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.data.export.DataExportInstructions;
import com.dbn.data.export.DataExportManager;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.execution.ExecutionResult;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.awt.event.ActionEvent;

public class ExportDataDialog extends DBNDialog<ExportDataForm> {
    private final ResultSetTable<?> table;
    private final ConnectionRef connection;
    private final DBObjectRef<?> sourceObject;

    public ExportDataDialog(ResultSetTable<?> table, @NotNull DBObject sourceObject) {
        this(table, sourceObject, sourceObject.getConnection());
    }

    public ExportDataDialog(ResultSetTable<?> table, @NotNull ExecutionResult<?> executionResult) {
        this(table, null, executionResult.getConnection());
    }


    private ExportDataDialog(ResultSetTable<?> table, @Nullable DBObject sourceObject, @NotNull ConnectionHandler connection) {
        super(connection.getProject(), "Export data", true);
        this.table = table;
        this.connection = connection.ref();
        this.sourceObject = DBObjectRef.of(sourceObject);
        init();
    }

    @NotNull
    @Override
    protected ExportDataForm createForm() {
        DBObject sourceObject = DBObjectRef.get(this.sourceObject);
        ConnectionHandler connection = this.connection.ensure();
        DataExportManager exportManager = DataExportManager.getInstance(connection.getProject());
        DataExportInstructions instructions = exportManager.getExportInstructions().clone();
        boolean hasSelection = table.getSelectedRowCount() > 1 || table.getSelectedColumnCount() > 1;
        instructions.setBaseName(table.getName());
        return new ExportDataForm(this, instructions, hasSelection, connection, sourceObject);
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{
                new DialogWrapperAction("Export") {
                    @Override
                    protected void doAction(ActionEvent actionEvent) {
                        doOKAction();
                    }
                },
                getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        getForm().validateEntries(
                () -> {
                    Project project = getProject();
                    ConnectionHandler connection = getConnection();
                    DataExportInstructions exportInstructions = getForm().getExportInstructions();
                    Progress.prompt(project, connection, true,
                            txt("prc.data.title.ExportingData"),
                            txt("prc.data.text.ExportingDataTo", exportInstructions.getFormat(), exportInstructions.getDestination()),
                            progress -> {
                                DataExportManager exportManager = DataExportManager.getInstance(project);
                                exportManager.setExportInstructions(exportInstructions);
                                exportManager.exportTableContent(
                                        table,
                                        exportInstructions,
                                        connection,
                                        () -> Dispatch.run(() -> ExportDataDialog.super.doOKAction()));
                            });
                }
        );
    }
}
