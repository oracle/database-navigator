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

package com.dbn.editor.session.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.database.DatabaseFeature;
import com.dbn.editor.session.SessionBrowser;
import com.dbn.editor.session.SessionBrowserFilter;
import com.dbn.editor.session.SessionBrowserFilterType;
import com.dbn.editor.session.SessionInterruptionType;
import com.dbn.editor.session.model.SessionBrowserColumnInfo;
import com.dbn.editor.session.model.SessionBrowserModel;
import com.dbn.editor.session.model.SessionBrowserModelCell;
import com.dbn.editor.session.model.SessionBrowserModelRow;
import com.dbn.editor.session.options.SessionBrowserSettings;
import com.dbn.editor.session.ui.table.SessionBrowserTable;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.util.Strings.cachedLowerCase;

public class SessionBrowserTableActionGroup extends DefaultActionGroup {
    boolean headerAction;
    private final WeakRef<SessionBrowser> sessionBrowser;
    private final WeakRef<SessionBrowserModelRow> row;

    public SessionBrowserTableActionGroup(SessionBrowser sessionBrowser, @Nullable SessionBrowserModelCell cell, SessionBrowserColumnInfo columnInfo) {
        this.sessionBrowser = WeakRef.of(sessionBrowser);
        SessionBrowserTable table = sessionBrowser.getBrowserTable();

        headerAction = cell == null;
        row = WeakRef.of(cell == null ? null : cell.getRow());
        SessionBrowserModel tableModel = sessionBrowser.getTableModel();

        add(new ReloadSessionsAction());
        if (table.getSelectedRowCount() > 0) {
            int rowCount = table.getSelectedRowCount();
            addSeparator();
            ConnectionHandler connection = getConnection();
            if (DatabaseFeature.SESSION_DISCONNECT.isSupported(connection)) {
                add(new DisconnectSessionAction(rowCount > 1));
            }
            add(new KillSessionAction(rowCount > 1));
        }

        addSeparator();

        if (cell != null) {
            Object userValue = cell.getUserValue();
            if (userValue instanceof String) {
                SessionBrowserFilterType filterType = columnInfo.getFilterType();
                if (filterType != null && tableModel != null) {
                    SessionBrowserFilter filter = tableModel.getFilter();
                    if (filter == null || Strings.isEmpty(filter.getFilterValue(filterType))) {
                        add(new FilterByAction(filterType, userValue.toString()));
                    }
                }
            }
        }

        if (tableModel != null && !tableModel.getState().getFilterState().isEmpty()) {
            add(new ClearFilterAction());
        }
    }

    @NotNull
    public SessionBrowser getSessionBrowser() {
        return sessionBrowser.ensure();
    }

    @Nullable
    public SessionBrowserModelRow getRow() {
        return row.get();
    }

    @NotNull
    private ConnectionHandler getConnection() {
        return getSessionBrowser().getConnection();
    }

    private class ReloadSessionsAction extends BasicAction {
        private ReloadSessionsAction() {
            super("Reload", null, Icons.ACTION_REFRESH);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            getSessionBrowser().loadSessions(true);
        }
    }

    private class KillSessionAction extends BasicAction {
        private KillSessionAction(boolean multiple) {
            super(multiple ? "Kill Sessions" : "Kill Session", null, Icons.ACTION_KILL_SESSION);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SessionBrowserModelRow row = getRow();
            if (row == null) return;

            getSessionBrowser().interruptSession(
                    row.getSessionIdentifier(),
                    SessionInterruptionType.TERMINATE);

        }
    }

    private class DisconnectSessionAction extends BasicAction {
        private DisconnectSessionAction(boolean multiple) {
            super(multiple ? "Disconnect Sessions" : "Disconnect Session", null, Icons.ACTION_DISCONNECT_SESSION);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SessionBrowserModelRow row = getRow();
            if (row == null) return;

            getSessionBrowser().interruptSession(
                    row.getSessionIdentifier(),
                    SessionInterruptionType.DISCONNECT);
        }
    }

    private class ClearFilterAction extends BasicAction {
        private ClearFilterAction() {
            super("Clear Filter", null, Icons.DATASET_FILTER_CLEAR);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            getSessionBrowser().clearFilter();
        }
    }

    private class FilterByAction extends BasicAction {
        private final SessionBrowserFilterType filterType;
        private final String name;
        private FilterByAction(SessionBrowserFilterType filterType, String name) {
            super("Filter by " + cachedLowerCase(filterType.name()) + " \"" + name + "\"", null, Icons.DATASET_FILTER);
            this.filterType = filterType;
            this.name = name;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            SessionBrowserModelRow row = getRow();
            if (row != null) {
                SessionBrowser sessionBrowser = getSessionBrowser();
                SessionBrowserModel tableModel = sessionBrowser.getTableModel();
                if (tableModel != null) {
                    SessionBrowserFilter filterState = tableModel.getState().getFilterState();
                    filterState.setFilterValue(filterType, name);

                    SessionBrowserSettings sessionBrowserSettings = sessionBrowser.getSettings();
                    if (sessionBrowserSettings.isReloadOnFilterChange()) {
                        sessionBrowser.loadSessions(false);
                    } else {
                        sessionBrowser.refreshTable();
                    }
                }
            }
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setText("Filter by " + cachedLowerCase(filterType.name()) + " \"" + name + "\"", false);
        }
    }
}
