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

import com.dbn.object.common.DBObject;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.management.ObjectManagementService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isValid;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.common.status.DBObjectStatus.DISABLED;

public class ObjectEnableDisableAction extends AnObjectAction<DBObject> {
    ObjectEnableDisableAction(DBObject object) {
        super(object);
    }

    @Override
    protected void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull DBObject object) {

        ObjectManagementService objectManagementService = ObjectManagementService.getInstance(project);

        if (objectManagementService.supports(object)) {
            boolean disabled = object.hasStatus(DISABLED);
            ObjectChangeAction changeAction = disabled ?
                    ObjectChangeAction.ENABLE :
                    ObjectChangeAction.DISABLE;
            objectManagementService.changeObject(object, changeAction,null);
        } else {
            throw new UnsupportedOperationException();
        }
    }


    @Override
    protected void update(
            @NotNull AnActionEvent e,
            @NotNull Presentation presentation,
            @NotNull Project project,
            @Nullable DBObject target) {

        if (isValid(target)) {
            boolean disabled = target.hasStatus(DISABLED);
            String text = disabled ?
                    txt("app.shared.action.Enable") :
                    txt("app.shared.action.Disable");

            presentation.setText(text);
            presentation.setVisible(true);
        } else {
            presentation.setVisible(false);
        }
    }
}
