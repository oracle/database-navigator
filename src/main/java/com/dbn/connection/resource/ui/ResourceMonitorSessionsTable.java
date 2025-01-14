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
import com.dbn.connection.ConnectionPool;
import com.dbn.connection.ConnectionType;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.session.DatabaseSession;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.text.DateFormatUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListSelectionModel;
import java.util.List;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

class ResourceMonitorSessionsTable extends DBNTable<ResourceMonitorSessionsTableModel> {
    ResourceMonitorSessionsTable(@NotNull DBNComponent parent, ResourceMonitorSessionsTableModel tableModel) {
        super(parent, tableModel, true);
        setDefaultRenderer(DatabaseSession.class, new CellRenderer());
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setCellSelectionEnabled(false);
        setRowSelectionAllowed(true);
        adjustColumnWidths();

        setAccessibleName(this, "Resource Monitor Sessions");
    }


    public static class CellRenderer extends DBNColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            DatabaseSession session = (DatabaseSession) value;
            ConnectionPool connectionPool = session.getConnection().getConnectionPool();

            if (session.isPool()) {
                int connectionPoolSize = connectionPool.getSize();
                SimpleTextAttributes textAttributes = connectionPoolSize == 0 ?
                        SimpleTextAttributes.GRAY_ATTRIBUTES :
                        SimpleTextAttributes.REGULAR_ATTRIBUTES;

                if (column == 0) {
                    append(session.getName(), textAttributes);
                    setIcon(session.getIcon());
                } else if (column == 1) {
                    append(connectionPoolSize == 0 ? "Not connected" : "Connected", textAttributes);
                } else if (column == 2) {
                    long lastAccessTimestamp = connectionPool.getLastAccess();
                    append(lastAccessTimestamp == 0 ? "Never" : DateFormatUtil.formatPrettyDateTime(lastAccessTimestamp), textAttributes);
                } else if (column == 3) {
                    append(connectionPoolSize + " / " + connectionPool.getPeakPoolSize(), textAttributes);
                } else if (column == 4) {
                    List<DBNConnection> conns = connectionPool.getConnections(ConnectionType.POOL);
                    int totalCursorsCount = conns.stream().mapToInt(c -> c.getActiveCursorCount()).sum();
                    append(Integer.toString(totalCursorsCount), textAttributes);
                } else if (column == 5) {
                    List<DBNConnection> conns = connectionPool.getConnections(ConnectionType.POOL);
                    int totalCursorsCount = conns.stream().mapToInt(c -> c.getCachedStatementCount()).sum();
                    append(Integer.toString(totalCursorsCount), textAttributes);
                }
            } else {
                DBNConnection conn = connectionPool.getSessionConnection(session.getId());
                SimpleTextAttributes textAttributes = conn == null ?
                        SimpleTextAttributes.GRAY_ATTRIBUTES :
                        SimpleTextAttributes.REGULAR_ATTRIBUTES;
                if (column == 0) {
                    append(session.getName(), textAttributes);
                    setIcon(session.getIcon());
                } else if (column == 1) {
                    append(conn == null ? "Not connected" : "Connected" + (conn.hasDataChanges() ? " - open transactions" : ""), textAttributes);
                } else if (column == 2) {
                    append(conn == null ? "" : DateFormatUtil.formatPrettyDateTime(conn.getLastAccess()), textAttributes);
                } else if (column == 4) {
                    append(conn == null ? "" : Integer.toString(conn.getActiveCursorCount()), textAttributes);
                } else if (column == 5) {

                }
            }
        }
    }
}
