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
import java.awt.BorderLayout;

public class ObjectListItemForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel removeActionPanel;
    private JPanel objectDetailsComponent;

    private final ObjectFactoryInputForm<?> inputForm;

    ObjectListItemForm(@NotNull ObjectListForm<?> parent, ObjectFactoryInputForm<?> inputForm) {
        super(parent);
        this.inputForm = inputForm;
        ActionToolbar actionToolbar = Actions.createActionToolbar(removeActionPanel, true, new RemoveObjectAction());
        removeActionPanel.add(actionToolbar.getComponent(), BorderLayout.NORTH);

    }

    @NotNull
    public ObjectListForm<?> getParentForm() {
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
            super("Remove " + getParentForm().getObjectType().getName(), null, Icons.ACTION_CLOSE);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            getParentForm().removeObjectPanel(ObjectListItemForm.this);
        }
    }

    ObjectFactoryInputForm<?> getObjectDetailsPanel() {
        return inputForm;
    }
}
