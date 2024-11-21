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

package com.dbn.object.common.sorting;

import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.dbn.object.type.DBObjectType;

public class DBColumnNameComparator extends DBObjectComparator<DBColumn> {

    public DBColumnNameComparator() {
        super(DBObjectType.COLUMN, SortingType.NAME);
    }

    @Override
    public int compare(DBColumn column1, DBColumn column2) {
        DBDataset dataset1 = column1.getDataset();
        DBDataset dataset2 = column2.getDataset();
        int result = compareRef(dataset1, dataset2);
        if (result == 0) {
            boolean primaryKey1 = column1.isPrimaryKey();
            boolean primaryKey2 = column2.isPrimaryKey();

            if (primaryKey1 && primaryKey2) {
                return compareName(column1, column2);
            } else if (primaryKey1) {
                return -1;
            } else if (primaryKey2){
                return 1;
            } else {
                return compareName(column1, column2);
            }
        }

        return result;
    }
}
