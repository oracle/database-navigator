/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.editor.data.nested;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.util.Dialogs;
import com.dbn.data.value.NestedTableValue;
import com.dbn.data.value.ValueAdapter;
import com.dbn.editor.data.filter.DatasetFilterInput;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.object.DBColumn;
import com.dbn.object.DBNestedTable;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Action;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static com.dbn.nls.NlsResources.txt;

public class DBNestedTableViewerDialog extends DBNDialog<DBNestedTableViewerForm> {
    private final DBObjectRef<DBNestedTable> nestedTable;
    private final DatasetFilterInput parentFilter;

    public DBNestedTableViewerDialog(@NotNull DBNestedTable nestedTable) {
        this(nestedTable, null);
    }

    public DBNestedTableViewerDialog(@NotNull DBNestedTable nestedTable, @Nullable DatasetFilterInput parentFilter) {
        super(nestedTable.getConnection(), txt("app.objects.action.ViewData"), true);
        this.nestedTable = DBObjectRef.of(nestedTable);
        this.parentFilter = parentFilter;
        setModal(false);
        setResizable(true);
        setDefaultSize(1000, 650);
        init();
    }

    public static void show(@NotNull DBNestedTable nestedTable) {
        showFiltered(nestedTable, null);
    }

    public static void show(@NotNull NestedTableValue nestedTableValue, @NotNull DatasetEditorModelCell cell) {
        DBNestedTable nestedTable = resolveNestedTable(nestedTableValue, cell);
        if (nestedTable == null) return;

        showFiltered(nestedTable, createParentFilter(nestedTable, cell));
    }

    @Nullable
    private static DBNestedTable resolveNestedTable(NestedTableValue value, DatasetEditorModelCell cell) {
        DBNestedTable nestedTable = value.getNestedTable();
        if (nestedTable == null) return null;

        DBColumn parentTableColumn = nestedTable.getParentTableColumn();
        return Objects.equals(parentTableColumn, value.getParentColumn()) ? nestedTable : null;
    }

    private static void showFiltered(@NotNull DBNestedTable nestedTable, @Nullable DatasetFilterInput parentFilter) {
        Dialogs.show(() -> new DBNestedTableViewerDialog(nestedTable, parentFilter));
    }

    @Nullable
    private static DatasetFilterInput createParentFilter(DBNestedTable nestedTable, DatasetEditorModelCell cell) {
        DBTable parentTable = nestedTable.getParentTable();
        List<DBColumn> keyColumns = parentTable.getPrimaryKeyColumns();
        if (keyColumns.isEmpty()) {
            keyColumns = parentTable.getUniqueKeyColumns();
        }
        if (keyColumns.isEmpty()) return null;

        DatasetFilterInput filter = new DatasetFilterInput(parentTable);
        for (DBColumn keyColumn : keyColumns) {
            DatasetEditorModelCell keyCell = cell.getRow().getCellForColumn(keyColumn);
            if (keyCell == null) return null;

            Object value = keyCell.getUserValue();
            if (value instanceof ValueAdapter<?> valueAdapter) {
                try {
                    value = valueAdapter.read();
                } catch (SQLException e) {
                    return null;
                }
            }
            filter.setColumnValue(keyColumn, value);
        }
        return filter;
    }

    @NotNull
    @Override
    protected DBNestedTableViewerForm createForm() {
        return new DBNestedTableViewerForm(this, DBObjectRef.ensure(nestedTable), parentFilter);
    }

    @Override
    protected Action[] initializeActions() {
        renameAction(getCancelAction(), txt("msg.shared.button.Close"));
        return actions(getCancelAction());
    }
}
