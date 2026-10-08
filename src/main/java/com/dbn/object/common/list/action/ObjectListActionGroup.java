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

package com.dbn.object.common.list.action;

import com.dbn.common.action.DefaultActionGroup;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.DatabaseEntity;
import com.dbn.object.DBDataset;
import com.dbn.object.DBSchema;
import com.dbn.object.action.ConsoleCreateAction;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.type.DBObjectType;
import com.dbn.options.general.WorkspaceFeature;
import com.dbn.sync.java.action.JavaObjectDownloadAction;
import com.dbn.sync.java.action.JavaResourceDownloadAction;
import com.intellij.openapi.project.Project;

import static com.dbn.database.DatabaseFeature.DEBUGGING;
import static com.dbn.database.DatabaseFeature.VECTOR_SEARCH;
import static com.dbn.object.type.DBObjectType.*;
import static com.dbn.object.type.DBObjectType.DATASET_TRIGGER;
import static com.dbn.object.type.DBObjectType.EVENT_TRIGGER;
import static com.dbn.options.general.WorkspaceFeature.*;
import static com.dbn.vfs.DBConsoleType.DEBUG;
import static com.dbn.vfs.DBConsoleType.SEARCH;
import static com.dbn.vfs.DBConsoleType.STANDARD;

public class ObjectListActionGroup extends DefaultActionGroup {

    public ObjectListActionGroup(DBObjectList objectList) {
        addListActions(objectList);
        addSchemaActions(objectList);
        addRootActions(objectList);
    }

    private void addListActions(DBObjectList objectList) {
        DBObjectType objectType = objectList.getObjectType();
        if (objectType != CONSOLE) {
            add(new ReloadObjectsAction(objectList));
            add(new ObjectListFilterActionGroup(objectList));
        }
    }

    private void addSchemaActions(DBObjectList objectList) {
        DBObjectType objectType = objectList.getObjectType();
        DatabaseEntity parentEntity = objectList.getParentEntity();

        if (parentEntity instanceof DBSchema schema) {
            addSeparator();
            Project project = schema.getProject();
            if (OJVM.isEnabled(project) && objectType == JAVA_CLASS) {
                add(new JavaObjectDownloadAction(schema));
            }
            if (OJVM.isEnabled(project) && objectType == JAVA_RESOURCE) {
                add(new JavaResourceDownloadAction(schema));
            }

            add(new CreateObjectAction(objectList));
        } else if (parentEntity instanceof DBDataset && objectType == DATASET_TRIGGER) {
            addSeparator();
            add(new CreateObjectAction(objectList));
        }

    }

    private void addRootActions(DBObjectList objectList) {
        DBObjectType objectType = objectList.getObjectType();
        DatabaseEntity parentElement = objectList.getParentEntity();

        if (parentElement instanceof DBObjectBundle) {
            if (objectType == CONSOLE) {
                ConnectionHandler connection = objectList.getConnection();
                addSeparator();
                add(new ConsoleCreateAction(connection, STANDARD));

                Project project = connection.getProject();
                if (DEBUGGER.isEnabled(project) && DEBUGGING.isSupported(connection)) {
                    add(new ConsoleCreateAction(connection, DEBUG));
                }
                if (VECTOR_TOOLBOX.isEnabled(project) && VECTOR_SEARCH.isSupported(connection)) {
                    add(new ConsoleCreateAction(connection, SEARCH));
                }
            } else if (objectType == EVENT_TRIGGER) {
                addSeparator();
                add(new CreateObjectAction(objectList));
            }
        }

    }
}
