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

package com.dbn.object.impl;

import com.dbn.object.DBJsonView;
import com.dbn.object.DBTable;
import com.dbn.object.common.list.DBObjectRelationImpl;
import com.dbn.object.type.DBObjectRelationType;
import lombok.Getter;

@Getter
class DBJsonViewTableRelation extends DBObjectRelationImpl<DBJsonView, DBTable> {
    private final short position;
    private final boolean root;
    private final boolean readonly;

    DBJsonViewTableRelation(DBJsonView jsonView, DBTable table, short position, boolean root, boolean readonly) {
        super(DBObjectRelationType.JSON_VIEW_TABLE, jsonView, table);
        this.position = position;
        this.root = root;
        this.readonly = readonly;
    }

    public DBJsonView getJsonView() {
        return getSourceObject();
    }

    public DBTable getTable() {
        return getTargetObject();
    }
}
