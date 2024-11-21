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

package com.dbn.execution.method.browser.action;

import com.dbn.common.action.ComboBoxAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.execution.method.browser.ui.MethodExecutionBrowserForm;
import com.dbn.object.DBSchema;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JComponent;

public class SchemaSelectDropdownAction extends ComboBoxAction {
    MethodExecutionBrowserForm browserComponent;

    public SchemaSelectDropdownAction(MethodExecutionBrowserForm browserComponent) {
        this.browserComponent = browserComponent;
    }

    @Override
    @NotNull
    protected DefaultActionGroup createPopupActionGroup(@NotNull JComponent component, @NotNull DataContext dataContext) {
        DefaultActionGroup actionGroup = new DefaultActionGroup();
        ConnectionHandler connection = browserComponent.getSettings().getConnection();
        if (connection != null) {
            for (DBSchema schema : connection.getObjectBundle().getSchemas()) {
                SchemaSelectAction schemaSelectAction = new SchemaSelectAction(browserComponent, schema);
                actionGroup.add(schemaSelectAction);
            }
        }
        return actionGroup;
    }

    @Override
    public void update(AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        String text = "Schema";
        Icon icon = null;

        DBSchema schema = browserComponent.getSettings().getSelectedSchema();
        if (schema != null) {
            text = schema.getName();
            icon = schema.getIcon();
        }

        presentation.setText(text, false);
        presentation.setIcon(icon);
    }
 }