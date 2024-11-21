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

package com.dbn.common.ui.list;

import com.dbn.common.ui.table.DBNEditableTableModel;
import com.dbn.common.util.Lists;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class EditableStringListModel extends DBNEditableTableModel {
    private final List<String> originalData;
    private final List<String> data;

    public EditableStringListModel(Collection<String> data, boolean sorted) {
        this.originalData = new ArrayList<>(data);
        this.data = new ArrayList<>(data);
        if (sorted) Collections.sort(this.data);
    }

    public boolean isChanged() {
        return !originalData.equals(data);
    }

    @Override
    public int getRowCount() {
        return data.size();
    }

    @Override
    public int getColumnCount() {
        return 1;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return "DATA";
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rowIndex < data.size() ? data.get(rowIndex) : null;
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (Lists.isOutOfBounds(data, rowIndex)) return;

        String currentValue = data.get(rowIndex);
        if (Objects.equals(currentValue, value)) return;

        data.set(rowIndex, (String) value);
        notifyListeners(rowIndex, rowIndex, columnIndex);
    }

    @Override
    public void insertRow(int rowIndex) {
        data.add(rowIndex, "");
        notifyListeners(rowIndex, data.size() + 1, -1);
    }

    @Override
    public void removeRow(int rowIndex) {
        if (Lists.isOutOfBounds(data, rowIndex)) return;
        if (rowIndex <= -1 || rowIndex >= data.size()) return;

        data.remove(rowIndex);
        notifyListeners(rowIndex, data.size() + 1, -1);
    }
}
