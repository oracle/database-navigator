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
import com.dbn.object.DBIndex;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.type.DBObjectType;

import java.util.LinkedList;
import java.util.List;

public class DBIndexNavigationInfoProvider extends DBObjectNavigationInfoProviderBase<DBIndex> {
    public DBIndexNavigationInfoProvider() {
        super(DBObjectType.INDEX);
    }

    @Override
    public List<DBObjectNavigationList<?>> createNavigationTargets(DBIndex index) {
        List<DBObjectNavigationList<?>> navigationLists = new LinkedList<>();

        List<DBColumn> columns = index.getColumns();
        if (!columns.isEmpty()) {
            navigationLists.add(DBObjectNavigationList.create("Columns", columns));
        }
        navigationLists.add(DBObjectNavigationList.create("Dataset", index.getDataset()));

        return navigationLists;
    }
}
