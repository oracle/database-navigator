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
import com.dbn.common.icon.Icons;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.factory.DatabaseObjectFactory;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

public class ObjectDropAction extends BasicAction {
    private final DBObjectRef<DBSchemaObject> object;

    public ObjectDropAction(DBSchemaObject object) {
        super(txt("app.objects.action.Drop"), null, Icons.ACTION_CLOSE);
        this.object = DBObjectRef.of(object);
    }

    public DBSchemaObject getObject() {
        return DBObjectRef.ensure(object);
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        DBSchemaObject object = getObject();
        DatabaseObjectFactory objectFactory = DatabaseObjectFactory.getInstance(object.getProject());
        objectFactory.dropObject(object);
    }
}