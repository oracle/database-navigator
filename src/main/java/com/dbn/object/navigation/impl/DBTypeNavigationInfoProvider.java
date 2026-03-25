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

package com.dbn.object.navigation.impl;

import com.dbn.data.type.DBDataType;
import com.dbn.object.DBType;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

public class DBTypeNavigationInfoProvider extends DBObjectNavigationInfoProviderBase<DBType> {
    public DBTypeNavigationInfoProvider() {
        super(DBObjectType.TYPE);
    }

    @Override
    public @Nullable DBObject getDefaultNavigationTarget(DBType type) {
        if (!type.isCollection()) return null;

        DBDataType dataType = type.getCollectionElementType();
        if (dataType == null) return null;
        if (!dataType.isDeclared()) return null;

        return dataType.getDeclaredType();

    }

    @Override
    public List<DBObjectNavigationList<?>> createNavigationTargets(DBType type) {
        List<DBObjectNavigationList<?>> navigationLists = new LinkedList<>();

        DBType superType = type.getSuperType();
        if (superType != null) {
            navigationLists.add(DBObjectNavigationList.create("Super Type", superType));
        }
        List<DBObject> types = type.getChildObjects(DBObjectType.TYPE_TYPE);
        if (!types.isEmpty()) {
            navigationLists.add(DBObjectNavigationList.create("Sub Types", types));
        }
        if (type.isCollection()) {
            DBDataType dataType = type.getCollectionElementType();
            if (dataType != null && dataType.isDeclared()) {
                DBType collectionElementType = dataType.getDeclaredType();
                if (collectionElementType != null) {
                    navigationLists.add(DBObjectNavigationList.create("Collection element type", collectionElementType));
                }
            }
        }

        return navigationLists;
    }
}
