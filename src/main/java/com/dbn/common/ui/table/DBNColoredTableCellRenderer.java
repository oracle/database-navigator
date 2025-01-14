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

package com.dbn.common.ui.table;

import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.ui.ColoredTableCellRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.AccessibleContext;
import javax.swing.JTable;
import javax.swing.border.Border;

import static com.dbn.common.ui.util.Borders.TEXT_FIELD_INSETS;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public abstract class DBNColoredTableCellRenderer extends ColoredTableCellRenderer {
    private static final Border DEFAULT_BORDER = TEXT_FIELD_INSETS; // use same border to avoid flickering when cell editing is toggled

    @Override
    protected final void customizeCellRenderer(@NotNull JTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
        try {
            setBorder(DEFAULT_BORDER);
            DBNTable dbnTable = (DBNTable) table;

            customizeCellRenderer(dbnTable, value, selected, hasFocus, row, column);
            customizeCellAccessibility(dbnTable, value, selected, hasFocus, row, column);
        } catch (ProcessCanceledException e){
            conditionallyLog(e);
        } catch (Throwable e){
            conditionallyLog(e);
        }
    }

    protected abstract void customizeCellRenderer(DBNTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column);

    private void customizeCellAccessibility(DBNTable table, @Nullable Object value, boolean selected, boolean hasFocus, int row, int column) {
        AccessibleContext accessibleContext = this.accessibleContext;
        if (accessibleContext == null) return; // accessibility not initialized, most likely not required

        DBNTableModel<Object> model = table.getModel();
        String columnValue = model.getPresentableValue(value, column);
        if (isEmpty(columnValue)) columnValue = "empty";

        String columnName = model.getColumnName(column);
        String accessibleName = isEmpty(columnName) ? columnValue : columnName + ": " + columnValue;
        accessibleContext.setAccessibleName(accessibleName);
    }
}
