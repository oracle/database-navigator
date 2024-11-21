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

package com.dbn.common.properties.ui;

import com.dbn.common.properties.KeyValueProperty;
import com.dbn.common.ui.table.DBNEditableTableModel;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import lombok.val;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PropertiesTableModel extends DBNEditableTableModel {
    private final List<KeyValueProperty> properties = new ArrayList<>();

    public PropertiesTableModel(Map<String, String> propertiesMap) {
        loadProperties(propertiesMap);
    }

    public void loadProperties(Map<String, String> propertiesMap) {
        for (val entry : propertiesMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            KeyValueProperty property = new KeyValueProperty(key, value);
            properties.add(property);
        }
    }

    public Map<String, String> exportProperties() {
        Map<String, String> propertiesMap = new HashMap<>();

        for (KeyValueProperty property : properties) {
            String key = property.getKey();
            if (!Strings.isEmptyOrSpaces(key)) {
                String value = Commons.nvl(property.getValue(), "");
                propertiesMap.put(key, value);
            }
        }
        return propertiesMap;
    }

    @Override
    public int getRowCount() {
        return properties.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnIndex == 0 ? "Property" :
               columnIndex == 1 ? "Value" : null;
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
        return
           columnIndex == 0 ? getKey(rowIndex) :
           columnIndex == 1 ? getValue(rowIndex) : null;
    }

    @Override
    public void setValueAt(Object o, int rowIndex, int columnIndex) {
        Object actualValue = getValueAt(rowIndex, columnIndex);
        if (!Commons.match(actualValue, o)) {
            KeyValueProperty property = properties.get(rowIndex);
            if (columnIndex == 0) {
                property.setKey((String) o);

            } else if (columnIndex == 1) {
                property.setValue((String) o);
            }

            notifyListeners(rowIndex, rowIndex, columnIndex);
        }
    }

    private String getKey(int rowIndex) {
        KeyValueProperty property = getProperty(rowIndex);
        return property.getKey();
    }

    private String getValue(int rowIndex) {
        KeyValueProperty property = getProperty(rowIndex);
        return property.getValue();
    }

    private KeyValueProperty getProperty(int rowIndex) {
        while (properties.size() <= rowIndex) {
            properties.add(new KeyValueProperty());
        }
        return properties.get(rowIndex);
    }

    @Override
    public void insertRow(int rowIndex) {
        properties.add(rowIndex, new KeyValueProperty());
        notifyListeners(rowIndex, properties.size()-1, -1);
    }

    @Override
    public void removeRow(int rowIndex) {
        if (properties.size() > rowIndex) {
            properties.remove(rowIndex);
            notifyListeners(rowIndex, properties.size()-1, -1);
        }
    }
}
