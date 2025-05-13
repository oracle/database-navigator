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

package com.dbn.event.listener.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.common.util.Strings;
import com.dbn.event.listener.filter.EventListenerFilter;
import com.dbn.event.listener.filter.EventListenerFilterType;
import com.dbn.event.listener.model.DataChangeListenerBundle;
import com.dbn.event.listener.ui.EventListenersForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.util.List;

import static com.dbn.event.listener.action.EventListenerActionUtil.getListenersForm;
import static com.dbn.nls.NlsResources.txt;

public abstract class EventListenerFilterAction extends ComboBoxAction implements DumbAware {
    private final EventListenerFilterType filterType;

    public EventListenerFilterAction(EventListenerFilterType filterType) {
        this.filterType = filterType;
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent component, @NotNull DataContext dataContext) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        EventListenersForm registrationsForm = EventListenerActionUtil.getListenersForm(dataContext);

        actionGroup.addSeparator();
        actionGroup.add(new SelectFilterValueAction(null));
        if (registrationsForm == null) return actionGroup;

        DBNTable<DataChangeListenerBundle> registrationsTable = registrationsForm.getListenersTable();
        DataChangeListenerBundle model = registrationsTable.getModel();
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


        EventListenersForm registrationsForm = getListenersForm(e);
        if (registrationsForm != null) {
            DataChangeListenerBundle listeners = registrationsForm.getListenersTable().getModel();
            EventListenerFilter filter = listeners.getFilter();

            if (filter != null) {
                String filterValue = filter.getFilterValue(filterType);
                if (Strings.isNotEmpty(filterValue)) {
                    text = filterValue;
                    icon = filterType.getIcon();
                }
            }
        }

        presentation.setEnabled(registrationsForm != null && !registrationsForm.isLoading());
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
            EventListenersForm registrationsForm = getListenersForm(e);
            if (registrationsForm == null) return;

            EventListenerFilter filter = registrationsForm.getListenersTable().getModel().getFilter();
            switch (filterType) {
                case USER: filter.setUser(filterValue); break;
                case TABLE: filter.setTable(filterValue); break;
                case STATUS: filter.setStatus(filterValue); break;
            }
            registrationsForm.refresh();

        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            if (filterValue == null) return;
            e.getPresentation().setText(filterValue, false);
        }
    }
 }