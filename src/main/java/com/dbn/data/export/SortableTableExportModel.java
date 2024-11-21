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

package com.dbn.data.export;

import com.dbn.common.util.Strings;
import com.dbn.data.grid.ui.table.sortable.SortableTable;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.model.sortable.SortableDataModel;
import com.dbn.data.model.sortable.SortableDataModelCell;
import com.dbn.data.type.DBNativeDataType;
import com.dbn.data.type.GenericDataType;
import com.google.common.base.CaseFormat;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dbn.common.util.Strings.cachedUpperCase;

@Getter
public class SortableTableExportModel implements DataExportModel{
    private final boolean selection;
    private final SortableTable<? extends SortableDataModel> table;

    private final Map<String, String> columnFriendlyNames = new HashMap<>();
    private final List<String> warnings = new ArrayList<>();

    private int[] selectedRows;
    private int[] selectedColumns;

    public SortableTableExportModel(boolean selection, SortableTable<? extends SortableDataModel>  table) {
        this.selection = selection;
        this.table = table;

        if (selection) {
            selectedRows = table.getSelectedRows();
            selectedColumns = table.getSelectedColumns();
        }
    }

    @Override
    public Project getProject() {
        return table.getProject();
    }

    @Override
    public String getTableName() {
        return table.getName();
    }

    @Override
    public int getColumnCount() {
        return selection ?
            selectedColumns.length :
            table.getModel().getColumnCount();
    }

    @Override
    public int getRowCount() {
        return selection ?
            selectedRows.length :
            table.getModel().getRowCount();
    }

    @Override
    public Object getValue(int rowIndex, int columnIndex) {
        int realRowIndex = getRealRowIndex(rowIndex);
        int realColumnIndex = getRealColumnIndex(columnIndex);
        SortableDataModelCell dataModelCell = (SortableDataModelCell) table.getModel().getValueAt(realRowIndex, realColumnIndex);
        return dataModelCell == null ? null : dataModelCell.getUserValue();
    }

    @Override
    public String getColumnName(int columnIndex) {
        int realColumnIndex = getRealColumnIndex(columnIndex);
        return table.getModel().getColumnName(realColumnIndex);
    }

    @Override
    public String getColumnFriendlyName(int columnIndex) {
        String columnName = getColumnName(columnIndex);
        return columnFriendlyNames.computeIfAbsent(columnName, n -> produceColumnFriendlyName(n));
    }

    @Nullable
    private static String produceColumnFriendlyName(String key) {
        if (Strings.isNotEmpty(key)) {
            key = cachedUpperCase(key.trim());
            if (key.matches("[A-Z][A-Z0-9_]*")) {
                key = key.replaceAll("_", " _");
                return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, key);
            }
        }
        return key;
    }


    @Override
    public GenericDataType getGenericDataType(int columnIndex) {
        int realColumnIndex = getRealColumnIndex(columnIndex);
        ColumnInfo columnInfo = table.getModel().getColumnInfo(realColumnIndex);
        DBNativeDataType nativeDataType = columnInfo.getDataType().getNativeType();

        return nativeDataType == null ?
                GenericDataType.LITERAL :
                nativeDataType.getGenericDataType();

    }

    /**
     * Returns the column index from the underlying sortable table model.
     */
    private int getRealColumnIndex(int columnIndex) {
        if (selection) {
            int selectedColumnIndex = selectedColumns[columnIndex];
            return table.convertColumnIndexToModel(selectedColumnIndex);
        } else {
            return table.convertColumnIndexToModel(columnIndex);
        }
    }

    private int getRealRowIndex(int rowIndex) {
        if (selection) {
            return selectedRows[rowIndex];
        } else {
            return rowIndex;
        }
    }

    @Override
    public void addWarning(String warning) {
        if (!warnings.contains(warning)) warnings.add(warning);
    }
}
