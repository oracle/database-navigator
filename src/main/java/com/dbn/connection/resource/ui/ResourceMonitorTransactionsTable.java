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

package com.dbn.connection.resource.ui;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.util.Editors;
import com.dbn.connection.transaction.PendingTransaction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListSelectionModel;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class ResourceMonitorTransactionsTable extends DBNTable<ResourceMonitorTransactionsTableModel> {

    ResourceMonitorTransactionsTable(@NotNull DBNComponent parent, ResourceMonitorTransactionsTableModel model) {
        super(parent, model, false);
        setDefaultRenderer(PendingTransaction.class, new CellRenderer());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellSelectionEnabled(true);
        adjustColumnWidths();
        addMouseListener(Mouse.listener().onClick(e -> clickEvent(e)));

        setAccessibleName(this, "Resource Monitor Transactions");
    }

    private void clickEvent(MouseEvent e) {
        if (e.getButton() != MouseEvent.BUTTON1 || e.getClickCount() != 1) return;

        int selectedRow = getSelectedRow();
        PendingTransaction change = (PendingTransaction) getModel().getValueAt(selectedRow, 0);
        VirtualFile virtualFile = change.getFile();
        if (virtualFile == null) return;

        Editors.openFileEditor(getProject(), virtualFile, true);
    }

    @Override
    protected void processMouseMotionEvent(MouseEvent e) {
        if (e.getID() != MouseEvent.MOUSE_DRAGGED && getChangeAtMouseLocation() != null) {
            setCursor(Cursors.handCursor());
        } else {
            super.processMouseMotionEvent(e);
            setCursor(Cursors.defaultCursor());
        }
    }

    private PendingTransaction getChangeAtMouseLocation() {
        Point location = MouseInfo.getPointerInfo().getLocation();
        location.setLocation(location.getX() - getLocationOnScreen().getX(), location.getY() - getLocationOnScreen().getY());

        int columnIndex = columnAtPoint(location);
        int rowIndex = rowAtPoint(location);
        if (columnIndex > -1 && rowIndex > -1) {
            return (PendingTransaction) getModel().getValueAt(rowIndex, columnIndex);
        }

        return null;
    }

    public static class CellRenderer extends DBNColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            PendingTransaction transaction = (PendingTransaction) value;
            if (column == 0) {
                setIcon(transaction.getFileIcon());
                append(transaction.getFilePath(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

            } else if (column == 1) {
                append(transaction.getChangesCount() + " uncommitted changes", SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
        }
    }
}
