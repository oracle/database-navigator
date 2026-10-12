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
import com.dbn.object.DBType;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

import javax.swing.table.TableCellEditor;
import java.util.HashMap;
import java.util.Map;

import static com.dbn.data.type.GenericDataType.ARRAY;
import static com.dbn.data.type.GenericDataType.DATE_TIME;
import static com.dbn.data.type.GenericDataType.LITERAL;
import static com.dbn.data.type.GenericDataType.NUMERIC;
import static com.dbn.data.type.GenericDataType.VECTOR;
import static com.dbn.nls.NlsResources.txt;

public class DatasetTableCellEditorFactory implements Disposable {
    private final Map<ColumnInfo, TableCellEditor> cache = new HashMap<>();

    public TableCellEditor getCellEditor(ColumnInfo columnInfo, DatasetEditorTable table) {
        return cache.computeIfAbsent(columnInfo, c -> createCellEditor(c, table, c.getDataType()));
    }

    private @Nullable TableCellEditor createCellEditor(ColumnInfo columnInfo, DatasetEditorTable table, DBDataType dataType) {
        if (dataType.isTable()) return createEditorForTableType(columnInfo, table);
        if (dataType.isNative()) return createEditorForNativeType(columnInfo, table);
        if (dataType.isDeclared()) return createEditorForDeclaredType(columnInfo, table);

        return null;
    }

    private static TableCellEditor createEditorForNativeType(ColumnInfo columnInfo, DatasetEditorTable table) {
        DataEditorSettings dataEditorSettings = DataEditorSettings.getInstance(table.getDatasetEditor().getProject());
        DBDataType dataType = columnInfo.getDataType();
        GenericDataType genericDataType = dataType.getGenericDataType();
        if (genericDataType == NUMERIC) {
            return new DatasetTableCellEditor(table);
        }

        if (genericDataType == DATE_TIME) {
            DatasetTableCellEditorWithPopup tableCellEditor = new DatasetTableCellEditorWithPopup(table);
            tableCellEditor.getEditorComponent().createCalendarPopup(false);
            return tableCellEditor;
        }

        if (genericDataType == ARRAY) {
            DatasetTableCellEditorWithPopup tableCellEditor = new DatasetTableCellEditorWithPopup(table);
            tableCellEditor.getEditorComponent().createArrayEditorPopup(false);
            return tableCellEditor;
        }

        if (genericDataType == VECTOR) {
            DatasetTableCellEditorWithPopup tableCellEditor = new DatasetTableCellEditorWithPopup(table);
            TextFieldWithPopup editorComponent = tableCellEditor.getEditorComponent();

            // VECTOR arrays with length > 0 are expected to be fixed-length (non-editable)
            boolean editable = dataType.getLength() == 0;
            if (editable)
                editorComponent.createArrayEditorPopup(false);  else
                editorComponent.createArrayViewerPopup(true);

            return tableCellEditor;
        }

        if (genericDataType == LITERAL) {
            long dataLength = dataType.getLength();


            if (dataLength < dataEditorSettings.getQualifiedEditorSettings().getTextLengthThreshold()) {
                DatasetTableCellEditorWithPopup tableCellEditor = new DatasetTableCellEditorWithPopup(table);

                DatasetEditorColumnInfo dseColumnInfo = (DatasetEditorColumnInfo) columnInfo;
                DBColumn column = dseColumnInfo.getColumn();
                TextFieldWithPopup editorComponent = tableCellEditor.getEditorComponent();
                DataEditorValueListPopupSettings valueListPopupSettings = dataEditorSettings.getValueListPopupSettings();

                if (!column.isPrimaryKey() && !column.isUniqueKey() && dataLength <= valueListPopupSettings.getDataLengthThreshold()) {
                    ListPopupValuesProvider valuesProvider = ListPopupValuesProviderBase.
                            create(txt("msg.dataEditor.title.PossibleValues"), () -> dseColumnInfo.getPossibleValues());

                    editorComponent.createValuesListPopup(valuesProvider, column, valueListPopupSettings.isShowPopupButton());
                }
                editorComponent.createTextEditorPopup(true);
                return tableCellEditor;
            } else {
                DatasetTableCellEditorWithTextEditor tableCellEditor = new DatasetTableCellEditorWithTextEditor(table);
                tableCellEditor.setEditable(false);
                return tableCellEditor;
            }

        }

        if (genericDataType.isLOB()) {
            DatasetTableCellEditorWithTextEditor tableCellEditor = new DatasetTableCellEditorWithTextEditor(table);
            tableCellEditor.setEditable(false);
            return tableCellEditor;
        }

        return null;
    }

    private static TableCellEditor createEditorForTableType(ColumnInfo columnInfo, DatasetEditorTable table) {
        return new DatasetTableCellEditorWithTableEditor(table);
    }

    private TableCellEditor createEditorForDeclaredType(ColumnInfo columnInfo, DatasetEditorTable table) {
        DBType declaredType = columnInfo.getDataType().getDeclaredType();
        if (declaredType == null || declaredType.isCollection()) return null;

        return new DatasetTableCellEditorWithTypeEditor(table);
    }

    @Override
    public void dispose() {
        for (TableCellEditor cellEditor : cache.values()) {
            if (cellEditor instanceof Disposable disposable) {
                Disposer.dispose(disposable);
            }
        }
        cache.clear();
    }
}
