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

package com.dbn.data.sorting;

import com.dbn.common.util.Cloneable;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class SortingInstruction implements Cloneable<SortingInstruction> {
    private int index;
    private String columnName;
    private SortDirection direction;

    public SortingInstruction(String columnName, SortDirection direction) {
        this.columnName = columnName.intern();
        this.direction = direction;
    }

    public void switchDirection() {
        if (direction == SortDirection.ASCENDING) {
            direction = SortDirection.DESCENDING;
        } else if (direction == SortDirection.DESCENDING) {
            direction = SortDirection.ASCENDING;
        }
    }

    @Override
    public SortingInstruction clone() {
        return new SortingInstruction(columnName, direction);
    }

    public DBColumn getColumn(DBDataset dataset) {
        return dataset.getColumn(columnName);
    }
}
