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

package com.dbn.object.properties.ui;

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.ui.table.DBNReadonlyTableModel;
import com.dbn.object.properties.PresentableProperty;

import java.util.ArrayList;
import java.util.List;

public class ObjectPropertiesTableModel extends StatefulDisposableBase implements DBNReadonlyTableModel {
    private List<PresentableProperty> presentableProperties = new ArrayList<>();

    ObjectPropertiesTableModel() {}

    ObjectPropertiesTableModel(List<PresentableProperty> presentableProperties) {
        this.presentableProperties = presentableProperties;
    }

    @Override
    public int getRowCount() {
        return presentableProperties.size();
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return
            columnIndex == 0 ? "Property" :
            columnIndex == 1 ? "Value" : null;
    }

    @Override public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }
    @Override public Object getValueAt(int rowIndex, int columnIndex) {
        return presentableProperties.get(rowIndex);
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
