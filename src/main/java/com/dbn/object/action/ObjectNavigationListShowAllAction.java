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

package com.dbn.object.action;

import com.dbn.browser.DatabaseBrowserManager;
import com.dbn.browser.ui.DatabaseBrowserTree;
import com.dbn.common.action.ProjectAction;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.ui.util.Popups.popupBuilder;
import static com.dbn.nls.NlsResources.txt;

public class ObjectNavigationListShowAllAction extends ProjectAction {
    private final DBObjectNavigationList navigationList;
    private final DBObject parentObject;

    ObjectNavigationListShowAllAction(DBObject parentObject, DBObjectNavigationList navigationList) {
        this.parentObject = parentObject;
        this.navigationList = navigationList;
    }

    @Override
    protected void actionPerformed(@NotNull AnActionEvent e, @NotNull Project project) {
        ObjectNavigationListActionGroup actionGroup =
                new ObjectNavigationListActionGroup(parentObject, navigationList, true);

        String listName = navigationList.getName();
        ListPopup popup = popupBuilder(actionGroup, e)
                .withTitle(listName)
                .withMaxRowCount(10)
                .withSpeedSearch()
                .build();

        DatabaseBrowserManager browserManager = DatabaseBrowserManager.getInstance(project);
        DatabaseBrowserTree activeBrowserTree = browserManager.getActiveBrowserTree();
        if (activeBrowserTree != null) {
            popup.showInCenterOf(activeBrowserTree);
        }
        //popup.show(DatabaseBrowserComponent.getInstance(project).getBrowserPanel().getTree());
    }

    @Override
    protected void update(@NotNull AnActionEvent e, @NotNull Project project) {
        e.getPresentation().setText(txt("app.objects.action.ShowAll"));
    }
}
