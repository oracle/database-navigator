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

package com.dbn.connection.config.action;

import com.dbn.common.action.ProjectAction;
import com.dbn.common.icon.Icons;
import com.dbn.connection.config.ConnectionBundleSettings;
import com.dbn.connection.config.ui.ConnectionListModel;
import com.dbn.data.sorting.SortDirection;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import javax.swing.JList;

import static com.dbn.nls.NlsResources.txt;

public class ConnectionsSortAction extends ProjectAction {
    private SortDirection currentSortDirection = SortDirection.ASCENDING;
    private final ConnectionBundleSettings connectionBundleSettings;
    private final JList list;

    public ConnectionsSortAction(JList list, ConnectionBundleSettings connectionBundleSettings) {
        this.list = list;
        this.connectionBundleSettings = connectionBundleSettings;
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        Icon icon;
        String text;
        if (currentSortDirection != SortDirection.ASCENDING) {
            icon = Icons.ACTION_SORT_ASC;
            text = txt("app.connection.action.SortConnectionsAscending");
        } else {
            icon = Icons.ACTION_SORT_DESC;
            text = txt("app.connection.action.SortConnectionsDescending");
        }
        Presentation presentation = e.getPresentation();
        presentation.setIcon(icon);
        presentation.setText(text);
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        currentSortDirection = currentSortDirection == SortDirection.ASCENDING ?
                SortDirection.DESCENDING :
                SortDirection.ASCENDING;

        if (list.getModel().getSize() > 0) {
            Object selectedValue = list.getSelectedValue();
            connectionBundleSettings.setModified(true);
            ConnectionListModel model = (ConnectionListModel) list.getModel();
            model.sort(currentSortDirection);
            list.setSelectedValue(selectedValue, true);
        }
    }
}
