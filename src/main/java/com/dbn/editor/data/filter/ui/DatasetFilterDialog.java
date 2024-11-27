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

package com.dbn.editor.data.filter.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.editor.data.DatasetEditorManager;
import com.dbn.editor.data.filter.DatasetBasicFilter;
import com.dbn.editor.data.filter.DatasetFilter;
import com.dbn.editor.data.filter.DatasetFilterGroup;
import com.dbn.editor.data.filter.DatasetFilterManager;
import com.dbn.editor.data.filter.DatasetFilterType;
import com.dbn.object.DBDataset;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class DatasetFilterDialog extends DBNDialog<DatasetFilterForm> {
    private final boolean automaticPrompt;
    private final DBObjectRef<DBDataset> dataset;
    private DatasetFilterGroup filterGroup;

    public DatasetFilterDialog(@NotNull DBDataset dataset, boolean automaticPrompt, boolean createNewFilter, DatasetFilterType defaultFilterType) {
        super(dataset.getProject(), "Data filters", true);
        this.dataset = DBObjectRef.of(dataset);
        this.automaticPrompt = automaticPrompt;
        setDefaultSize(1000, 700);
        DatasetFilterForm component = getForm();
        if ((createNewFilter || filterGroup.getFilters().isEmpty()) && defaultFilterType != DatasetFilterType.NONE) {
            DatasetFilter filter =
                    defaultFilterType == DatasetFilterType.BASIC ? filterGroup.createBasicFilter(true) :
                    defaultFilterType == DatasetFilterType.CUSTOM ? filterGroup.createCustomFilter(true) : null;

            component.getFilterList().setSelectedValue(filter, true);
        }
        init();
    }

    public DatasetFilterDialog(DBDataset dataset, DatasetBasicFilter basicFilter) {
        super(dataset.getProject(), "Data filters", true);
        this.dataset = DBObjectRef.of(dataset);
        this.automaticPrompt = false;
        getForm().getFilterList().setSelectedValue(basicFilter, true);
        init();
    }

    @NotNull
    @Override
    protected DatasetFilterForm createForm() {
        setModal(true);
        setResizable(true);
        DBDataset dataset = getDataset();
        DatasetFilterManager filterManager = DatasetFilterManager.getInstance(dataset.getProject());
        filterGroup = filterManager.getFilterGroup(dataset);
        return filterGroup.createConfigurationEditor();
    }

    @NotNull
    private DBDataset getDataset() {
        return DBObjectRef.ensure(dataset);
    }

    public DatasetFilterGroup getFilterGroup() {
        return filterGroup;
    }

    @Override
    @NotNull
    protected final Action[] createActions() {
        if (automaticPrompt) {
            return new Action[]{
                    getOKAction(),
                    new NoFilterAction(),
                    getCancelAction(),
                    getHelpAction()
            };
        } else {
            return new Action[]{
                    getOKAction(),
                    getCancelAction(),
                    getHelpAction()
            };
        }
    }

    private class NoFilterAction extends AbstractAction {
        public NoFilterAction() {
            super("No Filter");
            //putValue(DEFAULT_ACTION, Boolean.FALSE);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            doNoFilterAction();
        }
    }

    @Override
    public void doOKAction() {
        DatasetFilterForm component = getForm();
        Project project = getProject();
        DBDataset dataset = getDataset();
        try {
            component.applyFormChanges();
            DatasetFilterManager filterManager = DatasetFilterManager.getInstance(project);
            DatasetFilter activeFilter = component.getSelectedFilter();
            if (activeFilter == null) {
                activeFilter = DatasetFilterManager.EMPTY_FILTER;
            }
            filterManager.setActiveFilter(dataset, activeFilter);
        } catch (ConfigurationException e) {
            // TODO
            conditionallyLog(e);
        }
        super.doOKAction();
        if (!automaticPrompt) DatasetEditorManager.getInstance(project).reloadEditorData(dataset);
    }

    @Override
    public void doCancelAction() {
        DatasetFilterForm component = getForm();
        component.resetFormChanges();
        super.doCancelAction();
    }

    public void doNoFilterAction() {
        DatasetFilterForm component = getForm();
        component.resetFormChanges();
        DBDataset dataset = getDataset();
        Project project = getProject();
        DatasetFilterManager filterManager = DatasetFilterManager.getInstance(project);
        DatasetFilter activeFilter = filterManager.getActiveFilter(dataset);
        if (activeFilter == null) {
            activeFilter = DatasetFilterManager.EMPTY_FILTER;
            filterManager.setActiveFilter(dataset, activeFilter);
        }
        close(OK_EXIT_CODE);
    }

    @Override
    public void disposeInner() {
        filterGroup.disposeUIResources();
    }
}
