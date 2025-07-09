/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.event.notification.ui;

import com.dbn.common.action.DataKeys;
import com.dbn.common.color.Colors;
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.util.Borders;
import com.dbn.common.util.Actions;
import com.dbn.connection.ConnectionId;
import com.dbn.event.notification.EventNotificationListener;
import com.dbn.event.notification.filter.EventNotificationFilter;
import com.dbn.event.notification.model.DataChangeNotificationBundle;
import com.dbn.event.ui.EventMonitorDetailsForm;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.project.Project;
import com.intellij.util.ui.AsyncProcessIcon;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ClientProperty.NO_BORDER;
import static com.dbn.common.util.Conditional.when;

public class EventNotificationsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private JPanel controlPanel;
    private JPanel loadingIconPanel;
    private JLabel loadingLabel;
    private DBNScrollPane notificationsScrollPane;

    private @Getter EventNotificationsTable notificationsTable;

    public EventNotificationsForm(EventMonitorDetailsForm parent, DataChangeNotificationBundle events) {
        super(parent);
        initActionToolbar();
        initLoadIndicator();
        initTable(events);

        Project project = ensureProject();
        ProjectEvents.subscribe(project, this, EventNotificationListener.TOPIC, createEventNotificationListener());

        // start loading when the form is shown
        whenShown(() -> load());
    }

    private EventNotificationListener createEventNotificationListener() {
        return (connectionId, tableName) -> when(connectionId == getConnectionId(), () -> refresh());
    }

    private @Nullable ConnectionId getConnectionId() {
        return notificationsTable.getModel().getConnectionId();
    }

    private void initLoadIndicator() {
        loadingIconPanel.add(new AsyncProcessIcon("Loading"));
        loadingIconPanel.setVisible(false);
        loadingLabel.setVisible(false);
    }

    private void initActionToolbar() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.EventNotification.Controls");
        actionsPanel.add(actionToolbar.getComponent());
        controlPanel.setBorder(Borders.lineBorder(Colors.getTableGridColor(), 0, 0, 1, 0));
    }

    private void initTable(DataChangeNotificationBundle notifications) {
        notificationsTable = new EventNotificationsTable(this, notifications);
        notificationsScrollPane.setViewportView(notificationsTable);
        NO_BORDER.set(notificationsTable, true);
    }

    public void refresh() {
        if (isLoading()) return;
        load();
    }

    private void load() {
        markLoading(true);
        Background.run(() -> {
            try {
                DataChangeNotificationBundle model = notificationsTable.getModel();
                model.load();
            } catch (Exception e) {
                // TODO show load exception (maybe as a banner??)
            } finally {
                markLoading(false);
            }
        });
    }

    private void markLoading(boolean loading) {
        Dispatch.run(mainPanel, () -> {
            loadingIconPanel.setVisible(loading);
            loadingLabel.setVisible(loading);
            notificationsTable.setLoading(loading);
        });
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Nullable
    @Override
    public Object getData(@NotNull String dataId) {
        if (DataKeys.EVENT_NOTIFICATIONS_FORM.is(dataId)) return this;
        return null;
    }

    public boolean isLoading() {
        return notificationsTable.isLoading();
    }

    public void clearFilter() {
        EventNotificationFilter notificationsFilter = getFilter();
        notificationsFilter.clear();
        refresh();
    }

    public EventNotificationFilter getFilter() {
        return getNotificationsTable().getModel().getFilter();
    }
}
