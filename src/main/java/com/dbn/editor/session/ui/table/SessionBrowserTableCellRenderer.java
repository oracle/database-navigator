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

package com.dbn.editor.session.ui.table;

import com.dbn.common.dispose.Checks;
import com.dbn.common.dispose.Failsafe;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Commons;
import com.dbn.data.grid.ui.table.basic.BasicTableCellRenderer;
import com.dbn.editor.session.color.SessionBrowserTextAttributes;
import com.dbn.editor.session.model.SessionBrowserModelCell;
import com.dbn.editor.session.model.SessionBrowserModelRow;
import com.intellij.ui.SimpleTextAttributes;

import javax.swing.border.Border;
import java.awt.Color;

public class SessionBrowserTableCellRenderer extends BasicTableCellRenderer {

    @Override
    public SessionBrowserTextAttributes getAttributes() {
        return SessionBrowserTextAttributes.get();
    }

    @Override
    protected void customizeCellRenderer(DBNTable table, Object value, boolean isSelected, boolean hasFocus, int rowIndex, int columnIndex) {
        acquireState(table, isSelected, false, rowIndex, columnIndex);
        SessionBrowserModelCell cell = (SessionBrowserModelCell) value;
        SessionBrowserTable sessionBrowserTable = (SessionBrowserTable) table;

        if (Checks.allValid(cell, sessionBrowserTable)) {
            SessionBrowserModelRow row = cell.getRow();
            boolean isLoading = sessionBrowserTable.isLoading();

            boolean isCaretRow = table.getCellSelectionEnabled() && table.getSelectedRow() == rowIndex && table.getSelectedRowCount() == 1;
            boolean isConnected = Failsafe.nn(sessionBrowserTable.getSessionBrowser().getConnection()).isConnected();

            SessionBrowserTextAttributes attributes = getAttributes();
            SimpleTextAttributes textAttributes = attributes.getActiveSession(isCaretRow);

            if (isSelected) {
                textAttributes = attributes.getSelection();
            } else {
                if (isLoading || !isConnected) {
                    textAttributes = attributes.getLoadingData(isCaretRow);
                } else {
                    switch (row.getSessionStatus()) {
                        case ACTIVE: textAttributes = attributes.getActiveSession(isCaretRow); break;
                        case INACTIVE: textAttributes = attributes.getInactiveSession(isCaretRow); break;
                        case CACHED: textAttributes = attributes.getCachedSession(isCaretRow); break;
                        case SNIPED: textAttributes = attributes.getSnipedSession(isCaretRow); break;
                        case KILLED: textAttributes = attributes.getKilledSession(isCaretRow); break;
                    }
                }
            }

            Color background = Commons.nvl(textAttributes.getBgColor(), table.getBackground());
            Color foreground = Commons.nvl(textAttributes.getFgColor(), table.getForeground());


            Border border = Borders.lineBorder(background);

            setBorder(border);
            setBackground(background);
            setForeground(foreground);
            writeUserValue(cell, textAttributes, attributes);
        }
    }

    @Override
    public void setForeground(Color fg) {
        super.setForeground(fg);
    }
}
                                                                