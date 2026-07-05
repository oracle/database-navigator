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

package com.dbn.event.registration.action;

import com.dbn.common.action.BasicAction;
import com.dbn.common.action.ComboBoxAction;
import com.dbn.common.filter.FilterOption;
import com.dbn.common.ui.table.DBNTable;
import com.dbn.event.registration.filter.EventRegistrationFilter;
import com.dbn.event.registration.filter.EventRegistrationFilterType;
import com.dbn.event.registration.model.DataChangeRegistrationBundle;
import com.dbn.event.registration.ui.EventRegistrationsForm;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.util.List;

import static com.dbn.common.util.Commons.nvln;
import static com.dbn.event.registration.action.EventRegistrationActionUtil.getRegistrationsForm;
import static com.dbn.nls.NlsResources.txt;

public abstract class EventRegistrationFilterAction extends ComboBoxAction implements DumbAware {
    private final EventRegistrationFilterType filterType;

    public EventRegistrationFilterAction(EventRegistrationFilterType filterType, String text) {
        super(text);
        this.filterType = filterType;
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent component, @NotNull DataContext dataContext) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        EventRegistrationsForm registrationsForm = getRegistrationsForm(dataContext);

        actionGroup.addSeparator();
        actionGroup.add(new SelectFilterValueAction(null));
        if (registrationsForm == null) return actionGroup;

        DBNTable<DataChangeRegistrationBundle> registrationsTable = registrationsForm.getRegistrationsTable();
        DataChangeRegistrationBundle model = registrationsTable.getModel();

        List<FilterOption> filterOptions = model.geFilterOptions(filterType);
        for (FilterOption filterOption : filterOptions) {
            SelectFilterValueAction action = new SelectFilterValueAction(filterOption);
            actionGroup.add(action);
        }
        return actionGroup;
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        String text = filterType.getName();
        Icon icon = null;//Icons.DATASET_FILTER_EMPTY;


        EventRegistrationsForm registrationsForm = getRegistrationsForm(e);
        if (registrationsForm != null) {
            EventRegistrationFilter filter = registrationsForm.getFilter();

            if (filter != null) {
                FilterOption filterOption = filter.getFilterOption(filterType);
                if (filterOption != null) {
                    text = filterOption.getName();
                    icon = nvln(filterOption.getIcon(), filterType.getIcon());
                }
            }
        }

        presentation.setEnabled(registrationsForm != null && !registrationsForm.isLoading());
        presentation.setText(text, false);
        presentation.setIcon(icon);
    }

    private class SelectFilterValueAction extends BasicAction {
        private final FilterOption filterOption;

        public SelectFilterValueAction(FilterOption filterOption) {
            super(filterOption == null ? txt("app.shared.action.NoFilter") : filterOption.getName(), null, filterOption == null ? null : nvln(filterOption.getIcon(), filterType.getIcon()));
            this.filterOption = filterOption;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            EventRegistrationsForm registrationsForm = getRegistrationsForm(e);
            if (registrationsForm == null) return;

            EventRegistrationFilter filter = registrationsForm.getFilter();
            switch (filterType) {
                case USER: filter.setUser(filterOption); break;
                case TABLE: filter.setTable(filterOption); break;
                case STATUS: filter.setStatus(filterOption); break;
            }
            registrationsForm.refresh();

        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            if (filterOption == null) return;
            e.getPresentation().setText(filterOption.getName(), false);
        }
    }
 }
