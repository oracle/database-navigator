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

package com.dbn.connection.transaction.ui;

import com.dbn.common.dispose.Checks;
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.ui.util.Cursors;
import com.dbn.common.ui.util.Mouse;
import com.dbn.common.util.Editors;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.connection.transaction.PendingTransaction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListSelectionModel;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PendingTransactionsTable extends DBNTable<PendingTransactionsTableModel> {
    public PendingTransactionsTable(@NotNull PendingTransactionsDetailForm parent, @NotNull PendingTransactionsTableModel model) {
        super(parent, model, false);
        CellRenderer cellRenderer = new CellRenderer();
        setDefaultRenderer(PendingTransaction.class, cellRenderer);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellSelectionEnabled(true);
        adjustRowHeight(2);
        accommodateColumnsSize();
        addMouseListener(Mouse.listener().onClick(e -> clickEvent(e)));
    }

    private void clickEvent(MouseEvent e) {
        if (!Mouse.isMainSingleClick(e)) return;

        int selectedRow = getSelectedRow();
        PendingTransaction transaction = getModel().getValueAt(selectedRow, 0);
        VirtualFile virtualFile = transaction.getFile();
        if (!Checks.isValid(virtualFile)) return;

        Editors.openFileEditor(getProject(), virtualFile, true);
    }

    public List<DBNConnection> getSelectedConnections() {
        int[] selectedRows = getSelectedRows();
        if (selectedRows != null && selectedRows.length > 0) {
            Set<DBNConnection> connections = new LinkedHashSet<>();
            for (int selectedRow : selectedRows) {
                connections.add(getModel().getValueAt(selectedRow, 0).getConnection());
            }
            return new ArrayList<>(connections);
        }
        return Collections.emptyList();
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

    public PendingTransaction getChangeAtMouseLocation() {
        Point location = MouseInfo.getPointerInfo().getLocation();
        location.setLocation(location.getX() - getLocationOnScreen().getX(), location.getY() - getLocationOnScreen().getY());

        int columnIndex = columnAtPoint(location);
        int rowIndex = rowAtPoint(location);
        if (columnIndex > -1 && rowIndex > -1) {
            return getModel().getValueAt(rowIndex, columnIndex);
        }

        return null;
    }

    private static class CellRenderer extends DBNColoredTableCellRenderer {

        @Override
        protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            PendingTransactionsTableModel model = (PendingTransactionsTableModel) table.getModel();
            ConnectionHandler connection = model.getConnection();
            PendingTransaction transaction = (PendingTransaction) value;
            if (column == 0) {
                DatabaseSession session = connection.getSessionBundle().getSession(transaction.getSessionId());
                setIcon(session.getId().getIcon());
                append(session.getName());

            }
            else if (column == 1) {
                setIcon(transaction.getFileIcon());
                append(transaction.getFilePath(), SimpleTextAttributes.REGULAR_ATTRIBUTES);

            } else if (column == 2) {
                int changesCount = transaction.getChangesCount();
                append(changesCount == 1 ?
                        changesCount + " uncommitted change" :
                        changesCount + " uncommitted changes",
                        SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }
            setBorder(Borders.TEXT_FIELD_INSETS);
        }
    }
}
