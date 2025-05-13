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
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNScrollPane;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.util.Actions;
import com.dbn.event.notification.model.DataChangeEventBundle;
import com.dbn.event.ui.EventMonitorDetailsForm;
import com.intellij.openapi.actionSystem.ActionToolbar;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.dbn.common.ui.util.ClientProperty.NO_BORDER;

public class EventNotificationsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel actionsPanel;
    private DBNScrollPane notificationsScrollPane;

    private @Getter DBNTable<DataChangeEventBundle> notificationsTable;

    public EventNotificationsForm(EventMonitorDetailsForm parent, DataChangeEventBundle events) {
        super(parent);
        initTable(events);
        initActions();
    }

    private void initActions() {
        ActionToolbar actionToolbar = Actions.createActionToolbar(actionsPanel, true, "DBNavigator.ActionGroup.EventNotification.Controls");
        actionsPanel.add(actionToolbar.getComponent());
    }

    private void initTable(DataChangeEventBundle events) {
        notificationsTable = new NotificationEventTable(this, events);
        notificationsScrollPane.setViewportView(notificationsTable);
        NO_BORDER.set(notificationsTable, true);
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

    public void refresh() {

    }

    public boolean isLoading() {
        return notificationsTable.isLoading();
    }
}
