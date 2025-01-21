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

package com.dbn.connection.mapping.ui;

import com.dbn.common.action.BasicAction;
import com.dbn.common.color.Colors;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.thread.Progress;
import com.dbn.common.ui.Presentable;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNColoredTableCellRenderer;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.ui.table.DBNTableTransferHandler;
import com.dbn.common.util.Actions;
import com.dbn.common.util.Editors;
import com.dbn.common.util.Safe;
import com.dbn.connection.ConnectionBundle;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionManager;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.SchemaId;
import com.dbn.connection.mapping.FileConnectionContext;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.connection.session.DatabaseSession;
import com.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.ListSelectionModel;
import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Popups.popupBuilder;
import static com.dbn.common.util.Lists.convert;
import static com.dbn.connection.ConnectionHandler.isLiveConnection;
import static com.dbn.nls.NlsResources.txt;

public class FileConnectionMappingTable extends DBNTable<FileConnectionMappingTableModel> {
    private final FileConnectionContextManager manager;

    public FileConnectionMappingTable(@NotNull DBNComponent parent, FileConnectionMappingTableModel model) {
        super(parent, model, true);
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        setDefaultRenderer(FileConnectionContext.class, new CellRenderer());
        setTransferHandler(DBNTableTransferHandler.INSTANCE);
        initTableSorter();
        setCellSelectionEnabled(true);
        getRowSorter().toggleSortOrder(2);
        adjustColumnWidths();
        addMouseListener(new MouseListener());
        manager = FileConnectionContextManager.getInstance(getProject());

        setAccessibleName(this, "File Connection Mappings");
    }

    @Override
    protected int getMaxColumnWidth() {
        return 800;
    }

    @Override
    public void setModel(@NotNull TableModel dataModel) {
        super.setModel(dataModel);
        initTableSorter();
    }

    private static class CellRenderer extends DBNColoredTableCellRenderer {
        @Override
        protected void customizeCellRenderer(DBNTable table, Object value, boolean selected, boolean hasFocus, int row, int column) {
            FileConnectionContext entry = (FileConnectionContext) value;
            FileConnectionMappingTableModel model = (FileConnectionMappingTableModel) table.getModel();
            Object columnValue = model.getValue(entry, column);
            if (columnValue instanceof Presentable) {
                Presentable presentable = (Presentable) columnValue;
                //setIcon(presentable.getIcon());
            }

            if (columnValue instanceof ConnectionHandler ||
                    columnValue instanceof SchemaId ||
                    columnValue instanceof DatabaseSession) {
                //setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            } else if (columnValue instanceof VirtualFile) {
                VirtualFile virtualFile = (VirtualFile) columnValue;
                setIcon(virtualFile.getFileType().getIcon());
            }

            if (!selected) {
                ConnectionHandler connection = entry.getConnection();
                if (connection != null) {
                    Color color = connection.getEnvironmentType().getColor();
                    if (color != null) {
                        setBackground(Colors.softer(color, 30));
                        //setBackground(color.brighter());
                    }
                }
            }

            SimpleTextAttributes textAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
            String presentableValue = model.getPresentableValue(entry, column);
            append(presentableValue, textAttributes);
        }
    }

    public class MouseListener extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getButton() != MouseEvent.BUTTON1) return;

            int selectedRow = getSelectedRow();
            int selectedColumn = getSelectedColumn();
            if (selectedRow <= -1) return;

            FileConnectionContext mapping = (FileConnectionContext) getValueAt(selectedRow, 0);
            if (mapping == null) return;

            VirtualFile file = mapping.getFile();
            if (file == null) return;

            int clickCount = e.getClickCount();
            if (selectedColumn == 0 && clickCount == 2) {
                Editors.openFileEditor(getProject(), file, true);
            } else if (selectedColumn == 1) {
                promptConnectionSelector(mapping);
            } else if (selectedColumn == 3) {
                promptSchemaSelector(mapping);
            } else if (selectedColumn == 4) {
                promptSessionSelector(mapping);
            }
        }
    }


    private void promptConnectionSelector(@NotNull FileConnectionContext mapping) {
        Project project = getProject();
        ConnectionManager connectionManager = ConnectionManager.getInstance(project);
        ConnectionBundle connectionBundle = connectionManager.getConnectionBundle();
        VirtualFile file = mapping.getFile();

        List<AnAction> actions = new ArrayList<>();

        List<ConnectionHandler> connections = connectionBundle.getConnections();
        actions.addAll(convert(connections, c -> new ConnectionAction(file, c)));

        actions.add(Separator.create());
        Collection<ConnectionHandler> virtualConnections = connectionBundle.listVirtualConnections();
        actions.addAll(convert(virtualConnections, c -> new ConnectionAction(file, c)));

        actions.add(Separator.create());
        actions.add(new ConnectionAction(file, null));
        promptSelector("Connections", actions, a -> a instanceof ConnectionAction && ((ConnectionAction) a).getConnectionId() == mapping.getConnectionId());
    }

    private void promptSchemaSelector(@NotNull FileConnectionContext mapping) {
        ConnectionHandler connection = mapping.getConnection();
        if (!isLiveConnection(connection)) return;

        Project project = connection.getProject();
        Progress.modal(project, connection, true,
                txt("prc.fileContext.title.LoadingDataDictionary"),
                txt("prc.fileContext.text.LoadingSchemas"),
                progress -> {
                    VirtualFile file = mapping.getFile();
                    List<DBSchema> schemas = connection.getObjectBundle().getSchemas();
                    List<SchemaAction> actions = convert(schemas, s -> new SchemaAction(file, s.getIdentifier()));

                    promptSelector("Schemas", actions,  a -> a.getSchemaId() == mapping.getSchemaId());
                });
    }

    private void promptSessionSelector(@NotNull FileConnectionContext mapping) {
        ConnectionHandler connection = mapping.getConnection();
        if (!isLiveConnection(connection))  return;

        VirtualFile file = mapping.getFile();
        List<DatabaseSession> sessions = connection.getSessionBundle().getSessions();
        List<SessionAction> actions = convert(sessions, s -> new SessionAction(file, s));

        promptSelector("Sessions", actions, a -> a.getSession() == mapping.getSession());
    }

    private <T extends AnAction> void promptSelector(String title, List<T> actions, Condition<T> preselectCondition) {
        Dispatch.run(true, () -> {
            ListPopup popup = popupBuilder(actions, this).
                    withTitle(title).
                    withSpeedSearch().
                    withMaxRowCount(30).
                    withPreselectCondition(preselectCondition).
                    build();

            popup.showInScreenCoordinates(this, getPopupLocation());
        });
    }

    @NotNull
    private Point getPopupLocation() {
        Point location = getCellLocation(getSelectedRow(), getSelectedColumn());
        Rectangle rectangle = getCellRect(getSelectedRow(), getSelectedColumn(), true);
        location = new Point(
                (int) (location.getX() + rectangle.getWidth() / 2),
                (int) (location.getY() /*+ rectangle.getHeight()*/));
        return location;
    }



    private class ConnectionAction extends BasicAction {
        private final VirtualFile virtualFile;
        private final ConnectionRef connection;
        private ConnectionAction(VirtualFile virtualFile, ConnectionHandler connection) {
            super(
                Safe.call(connection, c -> c.getName(), txt("app.fileContext.action.NoConnection")), null,
                Safe.call(connection, c -> c.getIcon()));
            this.virtualFile = virtualFile;
            this.connection = ConnectionRef.of(connection);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ConnectionHandler connection = getConnection();
            manager.setConnection(virtualFile, connection);
            notifyModelChanges(virtualFile);
        }

        @Nullable
        private ConnectionHandler getConnection() {
            return ConnectionRef.get(connection);
        }

        public ConnectionId getConnectionId() {
            ConnectionHandler connection = getConnection();
            return connection == null ? null : connection.getConnectionId();
        }
    }

    @Getter
    private class SchemaAction extends BasicAction {
        private final VirtualFile virtualFile;
        private final SchemaId schemaId;
        private SchemaAction(VirtualFile virtualFile, SchemaId schemaId) {
            super(Actions.adjustActionName(schemaId.getName()), "", schemaId.getIcon());
            this.virtualFile = virtualFile;
            this.schemaId = schemaId;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            manager.setDatabaseSchema(virtualFile, schemaId);
            notifyModelChanges(virtualFile);
        }
    }

    @Getter
    private class SessionAction extends BasicAction {
        private final VirtualFile virtualFile;
        private final DatabaseSession session;
        private SessionAction(VirtualFile virtualFile, DatabaseSession session) {
            super(Actions.adjustActionName(session.getName()), "", session.getIcon());
            this.virtualFile = virtualFile;
            this.session = session;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            manager.setDatabaseSession(virtualFile, session);
            notifyModelChanges(virtualFile);
        }
    }

    private void notifyModelChanges(VirtualFile virtualFile) {
        FileConnectionMappingTableModel model = getModel();
        int rowIndex = model.indexOf(virtualFile);
        if (rowIndex > -1) {
            model.notifyRowChange(rowIndex);
        }
    }
}
