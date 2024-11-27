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

package com.dbn.data.grid.ui.table.resultSet.record;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import org.jetbrains.annotations.NotNull;

import javax.swing.Action;

public class ResultSetRecordViewerDialog extends DBNDialog<ResultSetRecordViewerForm> {
    private final ResultSetTable<?> table;
    private final boolean showDataTypes;
    public ResultSetRecordViewerDialog(ResultSetTable<?> table, boolean showDataTypes) {
        super(table.getProject(), "View record", true);
        this.table = table;
        this.showDataTypes = showDataTypes;
        setModal(true);
        setResizable(true);
        renameAction(getCancelAction(), "Close");
        init();
    }

    @NotNull
    @Override
    protected ResultSetRecordViewerForm createForm() {
        return new ResultSetRecordViewerForm(this, table, showDataTypes);
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
