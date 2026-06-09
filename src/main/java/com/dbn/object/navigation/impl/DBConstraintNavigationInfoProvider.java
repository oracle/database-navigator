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

import com.dbn.object.DBColumn;
import com.dbn.object.DBConstraint;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.type.DBObjectType;

import java.util.LinkedList;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class DBConstraintNavigationInfoProvider extends DBObjectNavigationInfoProviderBase<DBConstraint> {
    public DBConstraintNavigationInfoProvider() {
        super(DBObjectType.CONSTRAINT);
    }

    @Override
    public List<DBObjectNavigationList<?>> createNavigationTargets(DBConstraint constraint) {
        List<DBObjectNavigationList<?>> navigationLists = new LinkedList<>();

        List<DBColumn> columns = constraint.getColumns();
        if (!columns.isEmpty()) {
            navigationLists.add(DBObjectNavigationList.create(txt("app.objects.navigation.Columns"), columns));
        }

        DBConstraint foreignKeyConstraint = constraint.getForeignKeyConstraint();
        if (foreignKeyConstraint != null) {
            navigationLists.add(DBObjectNavigationList.create(txt("app.objects.navigation.ForeignKeyConstraint"), foreignKeyConstraint));
        }

        return navigationLists;
    }
}
