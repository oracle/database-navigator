/*
 * Copyright 2026 Oracle and/or its affiliates
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
import com.dbn.object.common.property.DBObjectProperty;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.management.ObjectManagementService;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isNotValid;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.common.status.DBObjectStatus.LOCKED;

public class ObjectLockUnlockAction extends AnObjectAction<DBObject> {
    public ObjectLockUnlockAction(DBObject object) {
        super(object);
    }

    @Override
    protected void actionPerformed(
            @NotNull AnActionEvent e,
            @NotNull Project project,
            @NotNull DBObject object) {

        ObjectManagementService objectManagementService = ObjectManagementService.getInstance(project);
        ObjectChangeAction changeAction = object.hasStatus(LOCKED) ?
                ObjectChangeAction.UNLOCK :
                ObjectChangeAction.LOCK;
        if (!objectManagementService.supports(object) || !isSupported(object)) {
            throw new UnsupportedOperationException();
        }
        objectManagementService.changeObject(object, changeAction, null);
    }

    @Override
    protected void update(
            @NotNull AnActionEvent e,
            @NotNull Presentation presentation,
            @NotNull Project project,
            @Nullable DBObject target) {

        if (!isSupported(target)) {
            presentation.setVisible(false);
            return;
        }

        ObjectChangeAction changeAction = target.hasStatus(LOCKED) ?
                ObjectChangeAction.UNLOCK :
                ObjectChangeAction.LOCK;
        presentation.setText(txt(changeAction == ObjectChangeAction.UNLOCK ?
                "app.shared.action.Unlock" : "app.shared.action.Lock"));
        presentation.setVisible(true);
    }

    public static boolean isSupported(@Nullable DBObject object) {
        if (isNotValid(object)) return false;
        if (!object.is(DBObjectProperty.LOCKABLE)) return false;

        ObjectChangeAction changeAction = object.hasStatus(LOCKED) ?
                ObjectChangeAction.UNLOCK :
                ObjectChangeAction.LOCK;
        return object.getConnection().getCompatibilityInterface().supportsObjectAction(
                object.getObjectType().getTypeId(), changeAction);
    }
}
