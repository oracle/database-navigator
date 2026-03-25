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

import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.navigation.DBObjectNavigationInfoProvider;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public class DBObjectNavigationInfoProviderBase<T extends DBObject> implements DBObjectNavigationInfoProvider<T> {
    private final DBObjectType objectType;

    public DBObjectNavigationInfoProviderBase(DBObjectType objectType) {
        this.objectType = objectType;
    }

    public DBObjectNavigationInfoProviderBase() {
        this(DBObjectType.ANY);
    }

    @Override
    public String getNavigationTooltipText(T object) {
        DBObject parentObject = object.getParentObject();
        if (parentObject == null) {
            return object.getTypeName();
        } else {
            return object.getTypeName() + " (" +
                    parentObject.getTypeName() + ' ' +
                    parentObject.getName() + ')';
        }
    }

    @Override
    public @Nullable DBObject getDefaultNavigationTarget(T object) {
        return null;
    }

    @Override
    public List<DBObjectNavigationList<?>> createNavigationTargets(T object) {
        return null;
    }
}
