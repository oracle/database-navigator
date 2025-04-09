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

package com.dbn.editor.data.ui.table.renderer;

import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Commons;
import com.dbn.data.grid.color.BasicTableTextAttributes;
import com.dbn.data.grid.ui.table.basic.BasicTableCellRenderer;
import com.dbn.editor.data.model.DatasetEditorColumnInfo;
import com.dbn.editor.data.model.DatasetEditorModelCell;
import com.dbn.editor.data.model.DatasetEditorModelRow;
import com.dbn.editor.data.ui.table.DatasetEditorTable;
import com.intellij.ui.SimpleTextAttributes;

import javax.swing.border.Border;
import java.awt.Color;

import static com.dbn.common.dispose.Checks.allValid;
import static com.dbn.editor.data.model.RecordStatus.DELETED;
import static com.dbn.editor.data.model.RecordStatus.INSERTING;
import static com.dbn.editor.data.model.RecordStatus.MODIFIED;
import static com.dbn.editor.data.model.RecordStatus.UPDATING;

public class DatasetEditorTableCellRenderer extends BasicTableCellRenderer {

    @Override
    protected void customizeCellRenderer(DBNTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        acquireState(table, isSelected, false, rowIndex, columnIndex);
        DatasetEditorModelCell cell = (DatasetEditorModelCell) value;
        DatasetEditorTable datasetEditorTable = (DatasetEditorTable) table;

        if (!allValid(cell, datasetEditorTable, datasetEditorTable.getProject())) return;

        DatasetEditorModelRow row = cell.getRow();
        DatasetEditorColumnInfo columnInfo = cell.getColumnInfo();

        boolean modified = cell.is(MODIFIED);
        boolean insertRow = row.is(INSERTING);
        boolean caretRow = !insertRow && table.getCellSelectionEnabled() && table.getSelectedRow() == rowIndex && table.getSelectedRowCount() == 1;
        boolean auditColumn = columnInfo != null && columnInfo.isAuditColumn();
        boolean connected = datasetEditorTable.getDatasetEditor().isConnected();

        BasicTableTextAttributes attributes = (BasicTableTextAttributes) getAttributes();
        SimpleTextAttributes textAttributes = getTextAttributes(cell, datasetEditorTable, rowIndex, isSelected);

        Color background = Commons.nvl(textAttributes.getBgColor(), table.getBackground());
        Color foreground = Commons.nvl(textAttributes.getFgColor(), table.getForeground());


        Border border = Borders.lineBorder(background);

        if (cell.hasError() && connected) {
            SimpleTextAttributes errorData = attributes.getErrorData();
            //border = Borders.lineBorder(SimpleTextAttributes.ERROR_ATTRIBUTES.getFgColor());
            border = Borders.lineBorder(errorData.getBgColor());
            background = errorData.getBgColor();
            foreground = errorData.getFgColor();
            textAttributes = textAttributes.derive(errorData.getStyle(), foreground, background, null);
        } else if (auditColumn && !isSelected) {
            SimpleTextAttributes auditDataAttr = attributes.getAuditData(modified, caretRow);
            foreground = Commons.nvl(auditDataAttr.getFgColor(), foreground);
            textAttributes = textAttributes.derive(textAttributes.getStyle(), foreground, background, null);
        }

        setBorder(border);
        setBackground(background);
        setForeground(foreground);
        writeUserValue(cell, textAttributes, attributes);
    }

    private SimpleTextAttributes getTextAttributes(DatasetEditorModelCell cell, DatasetEditorTable table, int rowIndex, boolean selected) {
        BasicTableTextAttributes attributes = (BasicTableTextAttributes) getAttributes();

        DatasetEditorModelRow row = cell.getRow();
        DatasetEditorColumnInfo columnInfo = cell.getColumnInfo();

        boolean dirty = table.getModel().isDirty();
        boolean loading = table.isLoading();
        boolean inserting = table.isInserting();

        boolean modified = cell.is(MODIFIED);
        boolean updating = cell.is(UPDATING);
        boolean deletedRow = row.is(DELETED);
        boolean insertRow = row.is(INSERTING);
        boolean caretRow = !insertRow && table.getCellSelectionEnabled() && table.getSelectedRow() == rowIndex && table.getSelectedRowCount() == 1;
        boolean auditColumn = columnInfo != null && columnInfo.isAuditColumn();
        boolean primaryKey = columnInfo != null && columnInfo.isPrimaryKey();
        boolean foreignKey = columnInfo != null && columnInfo.isForeignKey();
        boolean connected = table.getDatasetEditor().isConnected();

        if (loading) return attributes.getLoadingData(caretRow);
        if (selected) return
                table.hasFocus() ?
                        attributes.getSelection() :
                        attributes.getCaretRow();

        if (dirty) return attributes.getLoadingData(caretRow);
        if (!connected) return attributes.getLoadingData(caretRow);

        if (updating) return attributes.getUpdatingData(caretRow);
        if (deletedRow) return attributes.getDeletedData();
        if (inserting && !insertRow) return attributes.getReadonlyData(modified, caretRow);
        if (primaryKey) return attributes.getPrimaryKey(modified, caretRow);
        if (foreignKey) return attributes.getForeignKey(modified, caretRow);

        if (cell.isLobValue()) return attributes.getReadonlyData(modified, caretRow);
        if (cell.isArrayValue()) return attributes.getReadonlyData(modified, caretRow);
        if (auditColumn) return attributes.getAuditData(modified, caretRow);

        return attributes.getPlainData(modified, caretRow);
    }

    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
    }
}
                                                                