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

import com.dbn.common.util.Actions;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

import java.util.List;

public class ObjectListActionGroup extends DefaultActionGroup {

    private final ObjectListShowAction listShowAction;
    private final List<DBObject> objects;
    private final List<DBObject> recentObjects;

    public ObjectListActionGroup(ObjectListShowAction listShowAction, List<DBObject> objects, List<DBObject> recentObjects) {
        super("", true);
        this.objects = objects;
        this.recentObjects = recentObjects;
        this.listShowAction = listShowAction;

        if (objects != null) {
            buildNavigationActions();
        }
    }

    private void buildNavigationActions() {
        if (recentObjects != null) {
            for (DBObject object : recentObjects) {
                add(listShowAction.createObjectAction(object));
            }
            add(Actions.SEPARATOR);
        }

        for (DBObject object : objects) {
            if (recentObjects == null || !recentObjects.contains(object))
            add(listShowAction.createObjectAction(object));
        }
    }
}