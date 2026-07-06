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
import com.dbn.object.DBColumn;
import com.dbn.object.DBConstraint;
import com.dbn.object.DBIndex;
import com.dbn.object.DBTable;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedList;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class DBColumnNavigationInfoProvider extends DBObjectNavigationInfoProviderBase<DBColumn> {
    public DBColumnNavigationInfoProvider() {
        super(DBObjectType.COLUMN);
    }


    @Override
    public @Nullable DBObject getDefaultNavigationTarget(DBColumn column) {
        if (!column.isForeignKey()) return null;

        return column.getForeignKeyColumn();
    }

    @Override
    public List<DBObjectNavigationList<?>> createNavigationTargets(DBColumn column) {
        List<DBObjectNavigationList<?>> navigationLists = new LinkedList<>();
        DBDataType dataType = column.getDataType();

        if (dataType.isDeclared()) {
            navigationLists.add(DBObjectNavigationList.create(txt("app.objects.navigation.Type"), dataType.getDeclaredType()));
        }

        List<DBConstraint> constraints = column.getConstraints();
        if (!constraints.isEmpty()) {
            navigationLists.add(DBObjectNavigationList.create(txt("app.objects.navigation.Constraints"), constraints));
        }

        if (column.getParentObject() instanceof DBTable) {
            List<DBIndex> indexes = column.getIndexes();
            if (!indexes.isEmpty()) {
                navigationLists.add(DBObjectNavigationList.create(txt("app.objects.navigation.Indexes"), indexes));
            }

            if (column.isForeignKey()) {
                DBColumn foreignKeyColumn = column.getForeignKeyColumn();
                navigationLists.add(DBObjectNavigationList.create(txt("app.objects.navigation.ReferencedColumn"), foreignKeyColumn));
            }
        }

        if (column.isPrimaryKey()) {
            List<DBColumn> referencingColumns = column.getReferencingColumns();
            navigationLists.add(DBObjectNavigationList.create(txt("app.objects.navigation.ForeignKeyColumns"), referencingColumns));
        }
        return navigationLists;
    }
}
