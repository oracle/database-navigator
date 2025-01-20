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
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.list.ObjectListProvider;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

import java.util.List;

import static com.dbn.common.util.Unsafe.cast;

public class ObjectNavigationListActionGroup extends DefaultActionGroup {
    public static final int MAX_ITEMS = 30;
    private final DBObjectNavigationList<?> navigationList;
    private final DBObjectRef<?> parentObject;
    private final boolean showFullList;

    public ObjectNavigationListActionGroup(DBObject parentObject, DBObjectNavigationList navigationList, boolean showFullList) {
        this.parentObject = DBObjectRef.of(parentObject);
        this.navigationList = navigationList;
        this.showFullList = showFullList;

        if (navigationList.getObject() != null) {
            add(new NavigateToObjectAction(parentObject, navigationList.getObject()));
        } else {
            List<DBObject> objects = getObjects();
            int itemsCount = showFullList ? (objects == null ? 0 : objects.size()) : MAX_ITEMS;
            buildNavigationActions(itemsCount);
        }
    }

    public DBObject getParentObject() {
        return DBObjectRef.get(parentObject);
    }

    private <T extends DBObject> List<T> getObjects() {
        List<T> objects = cast(navigationList.getObjects());
        if (objects != null) return objects;

        ObjectListProvider<T> objectsProvider = cast(navigationList.getObjectsProvider());
        if (objectsProvider == null) return null;

        return objectsProvider.getObjects();
    }

    private void buildNavigationActions(int length) {
        List<DBObject> objects = getObjects();
        if (objects == null) return;


        DBObject parentObject = getParentObject();
        for (int i=0; i<length; i++) {
            if (i == objects.size()) {
                return;
            }
            DBObject object = objects.get(i);
            add(new NavigateToObjectAction(parentObject, object));
        }

        if (!showFullList && objects.size() > MAX_ITEMS) {
            addSeparator();
            add(new ObjectNavigationListShowAllAction(parentObject, navigationList));
        }
    }
}
