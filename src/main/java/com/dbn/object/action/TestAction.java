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
import com.dbn.editor.data.filter.global.DataDependencyPath;
import com.dbn.editor.data.filter.global.DataDependencyPathBuilder;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class TestAction extends BasicAction {
    private final DBObject object;
    public TestAction(DBObject object) {
        super("Test", "Test", null);
        this.object = object;
        setDefaultIcon(true);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        new Thread(() -> {
            if (object instanceof DBTable) {
                DBTable table = (DBTable) object;
                DBTable target = table.getSchema().getChildObject(DBObjectType.TABLE, "ALLOCATIONS", (short) 0, false);
                DataDependencyPath[] shortestPath = new DataDependencyPath[1];
                DataDependencyPathBuilder.buildDependencyPath(null, table.getColumns().get(0), target.getColumns().get(0), shortestPath);
            }
        }).start();
    }
}