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

package com.dbn.editor.data.filter;

import com.dbn.data.grid.options.DataGridSettings;
import com.dbn.data.sorting.SortDirection;
import com.dbn.data.sorting.SortingInstruction;
import com.dbn.data.sorting.SortingState;
import com.dbn.database.DatabaseCompatibility;
import com.dbn.database.JdbcProperty;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import org.jetbrains.annotations.NonNls;

import java.util.List;

import static com.dbn.common.dispose.Checks.isValid;

@NonNls
public class DatasetFilterUtil {

    public static void addOrderByClause(DBDataset dataset, StringBuilder buffer, SortingState sortingState) {
        DataGridSettings dataGridSettings = DataGridSettings.getInstance(dataset.getProject());
        boolean nullsFirst = dataGridSettings.getSortingSettings().isNullsFirst();
        List<SortingInstruction> sortingInstructions = sortingState.getInstructions();
        if (sortingInstructions.isEmpty()) return;

        buffer.append(" order by ");
        boolean instructionAdded = false;
        for (SortingInstruction sortingInstruction : sortingInstructions) {
            SortDirection sortDirection = sortingInstruction.getDirection();
            DBColumn column = dataset.getColumn(sortingInstruction.getColumnName());
            if (isValid(column) && !sortDirection.isIndefinite()) {
                String columnName = column.getName(true);
                DatabaseCompatibilityInterface compatibility = column.getCompatibilityInterface();
                String orderByClause = compatibility.getOrderByClause(columnName, sortDirection, nullsFirst);
                buffer.append(instructionAdded ? ", " : "");
                buffer.append(orderByClause);
                instructionAdded = true;
            }
        }
    }

    public static void createSelectStatement(DBDataset dataset, StringBuilder buffer) {
        buffer.append("select ");
        int index = 0;
        for (DBColumn column : dataset.getColumns()) {
            if (index > 0) {
                buffer.append(", ");
            }
            buffer.append(column.getName(true));
            index++;
        }
        buffer.append(" from ");
        buffer.append(dataset.getQualifiedName(true));
    }

    public static void createSimpleSelectStatement(DBDataset dataset, StringBuilder buffer) {
        DatabaseCompatibility compatibility = dataset.getConnection().getCompatibility();
        // TODO not implemented yet - returning always true at the moment
        boolean aliased = compatibility.isSupported(JdbcProperty.SQL_DATASET_ALIASING);

        String datasetName = dataset.getQualifiedName(true);

        if (aliased) {
            // IMPORTANT oracle jdbc seems to create readonly result-set if dataset is not aliased
            buffer.append("select a.* from ");
            buffer.append(datasetName);
            buffer.append(" a");
        } else {
            buffer.append("select * from ");
            buffer.append(datasetName);
        }
    }
}
