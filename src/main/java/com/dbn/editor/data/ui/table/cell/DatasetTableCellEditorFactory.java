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

package com.dbn.editor.data.ui.table.cell;

import com.dbn.common.dispose.Disposer;
import com.dbn.data.editor.ui.ListPopupValuesProvider;
import com.dbn.data.editor.ui.ListPopupValuesProviderBase;
import com.dbn.data.editor.ui.TextFieldWithPopup;
import com.dbn.data.model.ColumnInfo;
import com.dbn.data.type.DBDataType;
import com.dbn.data.type.GenericDataType;
import com.dbn.editor.data.model.DatasetEditorColumnInfo;
import com.dbn.editor.data.options.DataEditorSettings;
import com.dbn.editor.data.options.DataEditorValueListPopupSettings;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.dbn.object.DBColumn;
import com.intellij.openapi.Disposable;

import javax.swing.table.TableCellEditor;
import java.util.HashMap;
import java.util.Map;

public class DatasetTableCellEditorFactory implements Disposable {
    private final Map<ColumnInfo, TableCellEditor> cache = new HashMap<>();

    public TableCellEditor getCellEditor(ColumnInfo columnInfo, DatasetEditorTable table) {
        TableCellEditor tableCellEditor = cache.get(columnInfo);
        if (tableCellEditor == null) {
            DBDataType dataType = columnInfo.getDataType();
            tableCellEditor =
                dataType.isNative() ? createEditorForNativeType(columnInfo, table) :
                dataType.isDeclared() ? createEditorForDeclaredType(columnInfo, table) : null;
            cache.put(columnInfo, tableCellEditor);
        }
        return tableCellEditor;
    }

    private static TableCellEditor createEditorForNativeType(ColumnInfo columnInfo, DatasetEditorTable table) {
        DataEditorSettings dataEditorSettings = DataEditorSettings.getInstance(table.getDatasetEditor().getProject());
        DBDataType dataType = columnInfo.getDataType();
        GenericDataType genericDataType = dataType.getGenericDataType();
        if (genericDataType == GenericDataType.NUMERIC) {
            return new DatasetTableCellEditor(table);
        }
        else if (genericDataType == GenericDataType.DATE_TIME) {
            DatasetTableCellEditorWithPopup tableCellEditor = new DatasetTableCellEditorWithPopup(table);
            tableCellEditor.getEditorComponent().createCalendarPopup(false);
            return tableCellEditor;
        }
        else if (genericDataType == GenericDataType.ARRAY) {
            DatasetTableCellEditorWithPopup tableCellEditor = new DatasetTableCellEditorWithPopup(table);
            tableCellEditor.getEditorComponent().createArrayEditorPopup(false);
            return tableCellEditor;
        }
        else if (genericDataType == GenericDataType.LITERAL) {
            long dataLength = dataType.getLength();


            if (dataLength < dataEditorSettings.getQualifiedEditorSettings().getTextLengthThreshold()) {
                DatasetTableCellEditorWithPopup tableCellEditor = new DatasetTableCellEditorWithPopup(table);

                DatasetEditorColumnInfo dseColumnInfo = (DatasetEditorColumnInfo) columnInfo;
                DBColumn column = dseColumnInfo.getColumn();
                TextFieldWithPopup editorComponent = tableCellEditor.getEditorComponent();
                DataEditorValueListPopupSettings valueListPopupSettings = dataEditorSettings.getValueListPopupSettings();

                if (!column.isPrimaryKey() && !column.isUniqueKey() && dataLength <= valueListPopupSettings.getDataLengthThreshold()) {
                    ListPopupValuesProvider valuesProvider = ListPopupValuesProviderBase.
                            create("Possible Values", () -> dseColumnInfo.getPossibleValues());

                    editorComponent.createValuesListPopup(valuesProvider, column, valueListPopupSettings.isShowPopupButton());
                }
                editorComponent.createTextEditorPopup(true);
                return tableCellEditor;
            } else {
                DatasetTableCellEditorWithTextEditor tableCellEditor = new DatasetTableCellEditorWithTextEditor(table);
                tableCellEditor.setEditable(false);
                return tableCellEditor;
            }

        } else if (genericDataType.isLOB()) {
            DatasetTableCellEditorWithTextEditor tableCellEditor = new DatasetTableCellEditorWithTextEditor(table);
            tableCellEditor.setEditable(false);
            return tableCellEditor;
        }
        return null;
    }

    private TableCellEditor createEditorForDeclaredType(ColumnInfo columnInfo, DatasetEditorTable table) {
        return null;
    }

    @Override
    public void dispose() {
        for (TableCellEditor cellEditor : cache.values()) {
            if (cellEditor instanceof Disposable) {
                Disposable disposable = (Disposable) cellEditor;
                Disposer.dispose(disposable);
            }
        }
        cache.clear();
    }
}
