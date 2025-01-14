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

import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.util.Actions;
import com.dbn.data.grid.ui.table.basic.BasicTableCellRenderer;
import com.dbn.data.grid.ui.table.basic.BasicTableGutter;
import com.dbn.data.grid.ui.table.basic.BasicTableSelectionRestorer;
import com.dbn.data.grid.ui.table.resultSet.ResultSetTable;
import com.dbn.data.grid.ui.table.sortable.SortableTableHeaderRenderer;
import com.dbn.data.preview.LargeValuePreviewPopup;
import com.dbn.data.record.RecordViewInfo;
import com.dbn.editor.session.SessionBrowser;
import com.dbn.editor.session.action.SessionBrowserTableActionGroup;
import com.dbn.editor.session.model.SessionBrowserColumnInfo;
import com.dbn.editor.session.model.SessionBrowserModel;
import com.dbn.editor.session.model.SessionBrowserModelCell;
import com.dbn.editor.session.model.SessionBrowserModelRow;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionPopupMenu;
import com.intellij.ui.PopupMenuListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPopupMenu;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;

import static com.dbn.common.dispose.Failsafe.guarded;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class SessionBrowserTable extends ResultSetTable<SessionBrowserModel> {
    private final WeakRef<SessionBrowser> sessionBrowser;

    public SessionBrowserTable(DBNComponent parent, SessionBrowser sessionBrowser) {
        super(parent, createModel(sessionBrowser), false, createRecordInfo(sessionBrowser));
        this.sessionBrowser = WeakRef.of(sessionBrowser);

        getTableHeader().setDefaultRenderer(new SortableTableHeaderRenderer());
        getTableHeader().addMouseListener(new SessionBrowserTableHeaderMouseListener(this));
        addMouseListener(new SessionBrowserTableMouseListener(this));
        getSelectionModel().addListSelectionListener(listSelectionListener);
/*
        DataProvider dataProvider = sessionBrowser.getDataProvider();
        ActionUtil.registerDataProvider(this, dataProvider, false);
        ActionUtil.registerDataProvider(getTableHeader(), dataProvider, false);
*/
        setAccessibleName(this, "Session Browser");
    }

    @NotNull
    private static RecordViewInfo createRecordInfo(SessionBrowser sessionBrowser) {
        return new RecordViewInfo(sessionBrowser.getConnection().getName(), null);
    }

    @NotNull
    private static SessionBrowserModel createModel(SessionBrowser sessionBrowser) {
        return new SessionBrowserModel(sessionBrowser.getConnection());
    }

    @Override
    protected BasicTableCellRenderer createCellRenderer() {
        return new SessionBrowserTableCellRenderer();
    }

    @NotNull
    @Override
    public BasicTableSelectionRestorer createSelectionRestorer() {
        return new SelectionRestorer();
    }

    @Override
    public String getName() {
        return getSessionBrowser().getConnection().getName();
    }

    @Override
    protected BasicTableGutter<?> createTableGutter() {
        return new SessionBrowserTableGutter(this);
    }

    @Override
    public TableCellRenderer getCellRenderer(int row, int column) {
        return getCellRenderer();
    }

    @Override
    public void clearSelection() {
        Dispatch.run(() -> SessionBrowserTable.super.clearSelection());
    }

    @Override
    public boolean editCellAt(int row, int column, EventObject e) {
        return super.editCellAt(row, column, e);
    }

    @Override
    protected void initLargeValuePopup(LargeValuePreviewPopup viewer) {
        super.initLargeValuePopup(viewer);
    }

    @Override
    public int getColumnWidthBuffer() {
        return 22;
    }

    @NotNull
    public SessionBrowser getSessionBrowser() {
        return sessionBrowser.ensure();
    }

    private final ListSelectionListener listSelectionListener = e -> {
        if (e.getValueIsAdjusting()) return;

        guarded(this, t -> {
            t.snapshotSelection();
            SessionBrowser sessionBrowser = t.getSessionBrowser();
            sessionBrowser.updateDetails();
        });
    };

    @Override
    protected boolean showRecordViewDataTypes() {
        return false;
    }

    private class SelectionRestorer extends BasicTableSelectionRestorer{
        private Object sessionId;
        private int columnIndex;

        @Override
        public void snapshot() {
            if (!isRestoring()) {
                int selectedRowCount = getSelectedRowCount();
                int selectedColumnCount = getSelectedColumnCount();
                if (selectedRowCount == 1 && selectedColumnCount == 1) {
                    SessionBrowserModelRow selectedRow = getModel().getRowAtIndex(getSelectedRow());
                    sessionId = selectedRow == null ? null : selectedRow.getSessionId();
                    columnIndex = getSelectedColumn();
                } else if (selectedRowCount > 0 && selectedColumnCount > 0) {
                    sessionId = null;
                    columnIndex = -1;
                }
            }
        }

        @Override
        public void restore() {
            try {
                setRestoring(true);
                if (sessionId != null) {
                    int rowIndex = 0;
                    for (SessionBrowserModelRow row : getModel().getRows()) {
                        if (sessionId.equals(row.getSessionId())) {
                            selectCell(rowIndex, columnIndex);
                            break;
                        }
                        rowIndex++;
                    }
                }
            } finally {
                setRestoring(false);
            }
        }
    };


    /********************************************************
     *                        Popup                         *
     ********************************************************/
    public void showPopupMenu(
            MouseEvent event,
            SessionBrowserModelCell cell,
            SessionBrowserColumnInfo columnInfo) {
        Component eventSource = (Component) event.getSource();
        if (!eventSource.isShowing()) return;

        SessionBrowser sessionBrowser = getSessionBrowser();
        ActionGroup actionGroup = new SessionBrowserTableActionGroup(sessionBrowser, cell, columnInfo);
        ActionPopupMenu actionPopupMenu = Actions.createActionPopupMenu(SessionBrowserTable.this, actionGroup);
        JPopupMenu popupMenu = actionPopupMenu.getComponent();
        popupMenu.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                sessionBrowser.setPreventLoading(true);
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                sessionBrowser.setPreventLoading(false);
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                sessionBrowser.setPreventLoading(false);
            }
        });
        popupMenu.show(eventSource, event.getX(), event.getY());
    }
}
