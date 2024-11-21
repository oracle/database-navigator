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

package com.dbn.editor.data.state.column;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import com.dbn.object.DBColumn;
import com.dbn.object.DBDataset;
import com.dbn.object.type.DBObjectType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
@EqualsAndHashCode
public class DatasetColumnSetup implements PersistentStateElement, Cloneable<DatasetColumnSetup> {
    private List<DatasetColumnState> columnStates = new ArrayList<>();

    public void init(@Nullable List<String> columnNames, @NotNull DBDataset dataset) {
        if (columnNames == null) {
            columnNames = dataset.getChildObjectNames(DBObjectType.COLUMN);
        }
        List<DatasetColumnState> columnStates = new ArrayList<>();
        for (DBColumn column : dataset.getColumns()) {
            String columnName = column.getName();
            if (!column.isHidden() && columnNames.contains(columnName)) {
                DatasetColumnState columnsState = getColumnState(columnName);
                if (columnsState == null) {
                    columnsState = new DatasetColumnState(column);
                } else {
                    columnsState.init(column);
                }
                columnStates.add(columnsState);
            }
        }

        Collections.sort(columnStates);
        this.columnStates = columnStates;
    }


    public DatasetColumnState getColumnState(String columnName) {
        for (DatasetColumnState columnsState : columnStates) {
            if (Objects.equals(columnName, columnsState.getName())) {
                return columnsState;
            }
        }
        return null;
    }

    @Override
    public void readState(Element element) {
        if (element != null) {
            for (Element child : element.getChildren()) {
                String columnName = stringAttribute(child, "name");
                DatasetColumnState columnState = getColumnState(columnName);
                if (columnState == null) {
                    columnState = new DatasetColumnState(child);
                    columnStates.add(columnState);
                } else {
                    columnState.readState(child);
                }
            }
            Collections.sort(columnStates);
        }
    }

    @Override
    public void writeState(Element element) {
        for (DatasetColumnState columnState : columnStates) {
            Element childElement = newElement(element, "column");
            columnState.writeState(childElement);
        }
    }

    public void moveColumn(int fromIndex, int toIndex) {
        int visibleFromIndex = fromIndex;
        int visibleToIndex = toIndex;

        int visibleIndex = -1;
        for (int i=0; i< columnStates.size(); i++) {
            DatasetColumnState columnState = columnStates.get(i);
            if (columnState.isVisible()) {
                visibleIndex++;
                if (visibleIndex == fromIndex) visibleFromIndex = i;
                if (visibleIndex == toIndex) visibleToIndex = i;
            }
        }

        DatasetColumnState columnState = columnStates.remove(visibleFromIndex);
        columnStates.add(visibleToIndex, columnState);
        for (int i=0; i< columnStates.size(); i++) {
            columnStates.get(i).setPosition((short) i);
        }
    }

    public boolean isVisible(String name) {
        DatasetColumnState columnState = getColumnState(name);
        return columnState == null || columnState.isVisible();
    }

    @Override
    public DatasetColumnSetup clone() {
        DatasetColumnSetup clone = new DatasetColumnSetup();
        for (DatasetColumnState columnState : columnStates) {
            clone.columnStates.add(columnState.clone());
        }

        return clone;
    }
}
