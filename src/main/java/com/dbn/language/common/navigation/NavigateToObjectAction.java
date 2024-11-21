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

package com.dbn.language.common.navigation;

import com.dbn.common.action.BasicAction;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class NavigateToObjectAction extends BasicAction {
    private final DBObjectRef object;
    public NavigateToObjectAction(DBObject object, DBObjectType objectType) {
        super("Navigate to " + objectType.getName(), null, objectType.getIcon());
        this.object = DBObjectRef.of(object);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        if (object != null) {
            DBObject object = this.object.get();
            if (object != null) {
                object.navigate(true);
            }
        }

    }
}