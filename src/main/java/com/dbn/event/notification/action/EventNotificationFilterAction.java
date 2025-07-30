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

package com.dbn.event.notification.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.util.Strings;
import com.dbn.event.notification.filter.EventNotificationFilter;
import com.dbn.event.notification.filter.EventNotificationFilterType;
import com.dbn.event.notification.model.DataChangeNotificationBundle;
import com.dbn.event.notification.ui.EventNotificationsForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.util.List;

import static com.dbn.event.notification.action.EventNotificationActionUtil.getNotificationForm;
import static com.dbn.nls.NlsResources.txt;

public abstract class EventNotificationFilterAction extends ComboBoxAction implements DumbAware {
    private final EventNotificationFilterType filterType;

    public EventNotificationFilterAction(EventNotificationFilterType filterType) {
        this.filterType = filterType;
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent component, @NotNull DataContext dataContext) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        EventNotificationsForm notificationForm = getNotificationForm(dataContext);

        actionGroup.addSeparator();
        actionGroup.add(new SelectFilterValueAction(null));
        if (notificationForm == null) return actionGroup;

        DBNTable<DataChangeNotificationBundle> notificationsTable = notificationForm.getNotificationsTable();
        DataChangeNotificationBundle model = notificationsTable.getModel();
        List<String> filterValues = model.getDistinctValues(filterType);

        for (String filterValue : filterValues) {
            SelectFilterValueAction action = new SelectFilterValueAction(filterValue);
            actionGroup.add(action);
        }
        return actionGroup;
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        String text = filterType.getName();
        Icon icon = null;//Icons.DATASET_FILTER_EMPTY;


        EventNotificationsForm notificationForm = getNotificationForm(e);
        if (notificationForm != null) {
            DataChangeNotificationBundle notifications = notificationForm.getNotificationsTable().getModel();
            EventNotificationFilter filter = notifications.getFilter();

            if (filter != null) {
                String filterValue = filter.getFilterValue(filterType);
                if (Strings.isNotEmpty(filterValue)) {
                    text = filterValue;
                    icon = filterType.getIcon();
                }
            }
        }

        presentation.setEnabled(notificationForm != null && !notificationForm.isLoading());
        presentation.setText(text, false);
        presentation.setIcon(icon);
    }

    private class SelectFilterValueAction extends BasicAction {
        private final String filterValue;

        public SelectFilterValueAction(String filterValue) {
            super(filterValue == null ? txt("app.shared.action.NoFilter") : filterValue, null, filterValue == null ? null : filterType.getIcon());
            this.filterValue = filterValue;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            EventNotificationsForm notificationForm = getNotificationForm(e);
            if (notificationForm == null) return;

            EventNotificationFilter filter = notificationForm.getNotificationsTable().getModel().getFilter();
            switch (filterType) {
                case TABLE: filter.setTable(filterValue); break;
                case OPERATION: filter.setOperation(filterValue); break;
            }
            notificationForm.refresh();

        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            if (filterValue == null) return;
            e.getPresentation().setText(filterValue, false);
        }
    }
 }