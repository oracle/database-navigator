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

import com.dbn.object.DBSynonym;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DBSynonymNavigationInfoProvider extends DBObjectNavigationInfoProviderBase<DBSynonym> {
    public DBSynonymNavigationInfoProvider() {
        super(DBObjectType.SYNONYM);
    }

    @Override
    public String getNavigationTooltipText(DBSynonym synonym) {
        DBObject parentObject = synonym.getParentObject();
        if (parentObject == null) {
            return "unknown " + synonym.getTypeName();
        }

        DBObject underlyingObject = synonym.getUnderlyingObject();
        if (underlyingObject == null) {
            return "unknown " + synonym.getTypeName() +
                    " (" + parentObject.getTypeName() + " " + parentObject.getName() + ")";
        }

        return synonym.getTypeName() + " of " + underlyingObject.getName() + " " + underlyingObject.getTypeName() +
                " (" + parentObject.getTypeName() + " " + parentObject.getName() + ")";

    }

    @Override
    public @Nullable DBObject getDefaultNavigationTarget(DBSynonym synonym) {
        return synonym.getUnderlyingObject();
    }

    @Override
    public List<DBObjectNavigationList<?>> createNavigationTargets(DBSynonym synonym) {
        DBObject underlyingObject = synonym.getUnderlyingObject();
        if (underlyingObject == null) return null;

        return List.of(DBObjectNavigationList.create("Underlying " + underlyingObject.getTypeName(), underlyingObject));
    }
}
