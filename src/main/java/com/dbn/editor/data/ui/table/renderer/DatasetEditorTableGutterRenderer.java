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

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.table.DBNTableGutterRendererBase;
import com.dbn.data.grid.ui.table.basic.BasicTableGutter;
import com.dbn.editor.data.model.DatasetEditorModelRow;
import com.dbn.editor.data.ui.table.DatasetEditorTable;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.ListModel;
import java.awt.Color;

import static com.dbn.editor.data.model.RecordStatus.DELETED;
import static com.dbn.editor.data.model.RecordStatus.INSERTED;
import static com.dbn.editor.data.model.RecordStatus.INSERTING;
import static com.dbn.editor.data.model.RecordStatus.MODIFIED;

public class DatasetEditorTableGutterRenderer extends DBNTableGutterRendererBase {

    @Override
    protected void adjustListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        BasicTableGutter tableGutter = (BasicTableGutter) list;
        ListModel model = list.getModel();
        DatasetEditorModelRow row = (DatasetEditorModelRow) model.getElementAt(index);
        DatasetEditorTable table = (DatasetEditorTable) tableGutter.getTable();
        if (row != null) {
            Icon icon =
                    row.is(INSERTING) ? Icons.DATA_EDITOR_ROW_INSERT :
                    row.is(INSERTED) ? Icons.DATA_EDITOR_ROW_INSERTED :
                    row.is(DELETED) ? Icons.DATA_EDITOR_ROW_DELETED :
                    row.is(MODIFIED) ? Icons.DATA_EDITOR_ROW_MODIFIED :
                    table.getModel().is(MODIFIED) ? Icons.DATA_EDITOR_ROW_DEFAULT : null;

            if (icon == null || icon != iconLabel.getIcon()) {
                iconLabel.setIcon(icon);
            }
        }
        //lText.setFont(isSelected ? BOLD_FONT : REGULAR_FONT);

        boolean isCaretRow = table.getCellSelectionEnabled() && table.getSelectedRow() == index && table.getSelectedRowCount() == 1;
        Color background = isSelected ?
                table.getSelectionBackground() :
                isCaretRow ?
                        Colors.getTableCaretRowColor() :
                        table.getBackground();
        mainPanel.setBackground(background);
        iconLabel.setBackground(background);
        textLabel.setForeground(isSelected ?
                Colors.getTableSelectionForeground(cellHasFocus) :
                Colors.getTableGutterForeground());
    }
}
