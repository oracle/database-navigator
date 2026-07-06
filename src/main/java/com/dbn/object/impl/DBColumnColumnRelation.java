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

package com.dbn.object.impl;

import com.dbn.object.DBColumn;
import com.dbn.object.common.DBObject;
import com.dbn.object.common.list.DBObjectRelationImpl;
import com.dbn.object.type.DBObjectRelationType;

class DBColumnColumnRelation extends DBObjectRelationImpl<DBColumn, DBColumn> {
    DBColumnColumnRelation(DBColumn sourceColumn, DBColumn targetColumn) {
        super(DBObjectRelationType.COLUMN_COLUMN, sourceColumn, targetColumn);
    }

    @Override
    public DBObject getRelationHolder() {
        DBColumn column = getTargetColumn();
        DBObject parentObject = column.getParentObject();
        return parentObject == null ? column : parentObject;
    }

    public DBColumn getSourceColumn() {
        return getSourceObject();
    }

    public DBColumn getTargetColumn() {
        return getTargetObject();
    }
}
