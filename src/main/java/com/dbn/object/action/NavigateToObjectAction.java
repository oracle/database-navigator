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

import com.dbn.common.action.BasicAction;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Presentation;
import org.jetbrains.annotations.NotNull;

public class NavigateToObjectAction extends BasicAction {
    private final DBObjectRef<DBObject> objectRef;

    public NavigateToObjectAction(DBObject object) {
        super();
        Presentation presentation = getTemplatePresentation();
        presentation.setText(object.getName(), false);
        presentation.setIcon(object.getIcon());
        this.objectRef = DBObjectRef.of(object);
    }

    public NavigateToObjectAction(DBObject sourceObject, DBObject object) {
        super();
        this.objectRef = DBObjectRef.of(object);

        Presentation presentation = getTemplatePresentation();
        if (object instanceof DBJavaParameter) {
            presentation.setText(object.getName(), true);
        } else {
            presentation.setText(
                    sourceObject != object.getParentObject() ?
                            object.getQualifiedName() :
                            object.getName(), true);
        }
        presentation.setIcon(object.getIcon());
        presentation.setDescription(object.getTypeName());
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DBObjectRef.ensure(objectRef).navigate(true);
    }
}
