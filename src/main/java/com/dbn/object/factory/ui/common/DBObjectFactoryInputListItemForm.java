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

package com.dbn.object.factory.ui.common;

import com.dbn.common.action.BasicAction;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.util.Actions;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

public class DBObjectFactoryInputListItemForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel removeActionPanel;
    private JPanel objectDetailsComponent;

    private final DBObjectFactoryInputForm<?> inputForm;

    DBObjectFactoryInputListItemForm(@NotNull DBObjectFactoryInputListForm<?> parent, DBObjectFactoryInputForm<?> inputForm) {
        super(parent);
        this.inputForm = inputForm;
        if (!inputForm.isReadonly()) {
            ActionToolbar actionToolbar = Actions.createActionToolbar(removeActionPanel, true, new RemoveObjectAction());
            removeActionPanel.add(actionToolbar.getComponent());
        }
    }

    @NotNull
    public DBObjectFactoryInputListForm<?> getParentForm() {
        return ensureParentComponent();
    }

    @NotNull
    @Override
    public JPanel getMainComponent(){
        return mainPanel;
    }

    private void createUIComponents() {
        objectDetailsComponent = (JPanel) inputForm.getComponent();
    }

    public class RemoveObjectAction extends BasicAction {
        RemoveObjectAction() {
            super(txt("app.objects.action.RemoveObject", getObjectTypeName()), null, Icons.ACTION_DELETE);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            getParentForm().removeObjectPanel(DBObjectFactoryInputListItemForm.this);
        }
    }

    private @NotNull String getObjectTypeName() {
        return getParentForm().getObjectType().getName();
    }

    DBObjectFactoryInputForm<?> getObjectDetailsPanel() {
        return inputForm;
    }
}
