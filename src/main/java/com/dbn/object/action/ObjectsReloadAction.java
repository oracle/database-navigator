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
import com.dbn.common.thread.Background;
import com.dbn.connection.ConnectionAction;
import com.dbn.object.common.list.DBObjectList;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class ObjectsReloadAction extends BasicAction {

    private final DBObjectList<?> objectList;

    ObjectsReloadAction(DBObjectList<?> objectList) {
        super((objectList.isLoaded() ? "Reload " : "Load ") + objectList.getName());
        this.objectList = objectList;
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        String listName = objectList.getName();
        boolean loaded = objectList.isLoaded();

        String description = loaded ? "reloading the " + listName : "loading the " + listName;
        ConnectionAction.invoke(description, true, objectList, action -> reloadObjectList());
    }

    private void reloadObjectList() {
        Background.run(objectList.getProject(), () -> {
            objectList.getConnection().getMetaDataCache().reset();
            objectList.reload();
        });
    }
}