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

package com.dbn.data.export.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.util.Dialogs;
import com.dbn.data.export.ui.ExportDataDialog;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.object.DBDataset;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ExportDataAction extends BasicAction {
    private final WeakRef<ResultSetTable<?>> table;
    private final DBObjectRef<DBDataset> dataset;

    public ExportDataAction(ResultSetTable<?> table, DBDataset dataset) {
        super("Export Data", null, Icons.DATA_EXPORT);
        this.table = WeakRef.of(table);
        this.dataset = DBObjectRef.of(dataset);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Dialogs.show(() -> new ExportDataDialog(table.ensure(), dataset.ensure()));
    }
}
