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

package com.dbn.object.filter.quick.ui;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.filter.quick.ObjectQuickFilter;
import com.dbn.object.filter.quick.ObjectQuickFilterManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractAction;
import javax.swing.Action;
import java.awt.event.ActionEvent;

public class ObjectQuickFilterDialog extends DBNDialog<ObjectQuickFilterForm> {
    private final DBObjectList<?> objectList;
    public ObjectQuickFilterDialog(Project project, DBObjectList<?> objectList) {
        super(project, "Quick filter", true);
        this.objectList = objectList;
        setModal(true);
        //setResizable(false);
        renameAction(getOKAction(), "Apply");
        init();
    }

    @NotNull
    @Override
    protected ObjectQuickFilterForm createForm() {
        return new ObjectQuickFilterForm(this, objectList);
    }

    @Override
    protected String getDimensionServiceKey() {
        return null;
    }

    @NotNull
    @Override
    protected Action[] createActions() {
        return new Action[]{
                getOKAction(),
                new AbstractAction("Clear Filters") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        getForm().getFilter().clear();
                        doOKAction();
                    }
                },
                getCancelAction()
        };
    }

    @Override
    public void doOKAction() {
        try {
            ObjectQuickFilterManager quickFilterManager = ObjectQuickFilterManager.getInstance(getProject());
            ObjectQuickFilter<?> filter = getForm().getFilter();
            quickFilterManager.applyFilter(getForm().getObjectList(), filter.isEmpty() ? null : filter);
        } finally {
            super.doOKAction();
        }
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }
}
