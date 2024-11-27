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

package com.dbn.execution.script.options.ui;

import com.dbn.common.ui.table.DBNEditableTableModel;
import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.connection.DatabaseType;
import com.dbn.execution.script.CmdLineInterface;
import com.dbn.execution.script.CmdLineInterfaceBundle;
import com.intellij.openapi.options.ConfigurationException;
import lombok.Getter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class CmdLineInterfacesTableModel extends DBNEditableTableModel {
    private CmdLineInterfaceBundle bundle;

    public CmdLineInterfacesTableModel(CmdLineInterfaceBundle bundle) {
        this.bundle = bundle.clone();
    }

    public void setBundle(CmdLineInterfaceBundle bundle) {
        this.bundle = bundle.clone();
        notifyListeners(0, bundle.size(), -1);
    }

    @Override
    public int getRowCount() {
        return bundle.size();
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnIndex == 0 ? "Database Type" :
               columnIndex == 1 ? "Name" :
               columnIndex == 2 ? "Executable Path" : null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;

    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        CmdLineInterface environmentType = getInterface(rowIndex);
        return
           columnIndex == 0 ? environmentType.getDatabaseType() :
           columnIndex == 1 ? environmentType.getName() :
           columnIndex == 2 ? environmentType.getExecutablePath() : null;
    }

    @Override
    public void setValueAt(Object o, int rowIndex, int columnIndex) {
        Object actualValue = getValueAt(rowIndex, columnIndex);
        if (!Commons.match(actualValue, o)) {
            CmdLineInterface cmdLineInterface = bundle.get(rowIndex);
            if (columnIndex == 0) {
                DatabaseType databaseType = (DatabaseType) o;
                cmdLineInterface.setDatabaseType(databaseType);
            } else if (columnIndex == 1) {
                cmdLineInterface.setName((String) o);
            } else if (columnIndex == 2) {
                cmdLineInterface.setExecutablePath((String) o);
            }

            notifyListeners(rowIndex, rowIndex, columnIndex);
        }
    }

    public Set<String> getInterfaceNames() {
        return bundle.getInterfaceNames();
    }


    private CmdLineInterface getInterface(int rowIndex) {
        while (bundle.size() <= rowIndex) {
            bundle.add(new CmdLineInterface());
        }
        return bundle.get(rowIndex);
    }

    public void addInterface(CmdLineInterface cmdLineInterface) {
        bundle.add(cmdLineInterface);
        int rowIndex = bundle.size() - 1;
        notifyListeners(rowIndex, rowIndex, -1);
    }

    @Override
    public void insertRow(int rowIndex) {
        bundle.add(rowIndex, new CmdLineInterface());
        notifyListeners(rowIndex, bundle.size()-1, -1);
    }

    @Override
    public void removeRow(int rowIndex) {
        if (bundle.size() > rowIndex) {
            bundle.remove(rowIndex);
            notifyListeners(rowIndex, bundle.size()-1, -1);
        }
    }

    public void validate() throws ConfigurationException {
        Set<String> names = new HashSet<>();
        for (CmdLineInterface cmdLineInterface : bundle.getInterfaces()) {
            String name = cmdLineInterface.getName();
            if (Strings.isEmpty(name)) {
                throw new ConfigurationException("Please provide names for each Command-Line Interface.");
            } else if (names.contains(name)) {
                throw new ConfigurationException("Please provide unique Command-Line Interface names.");
            } else {
                names.add(name);
            }
        }

        for (CmdLineInterface cmdLineInterface : bundle.getInterfaces()) {
            if (Strings.isEmpty(cmdLineInterface.getExecutablePath())) {
                throw new ConfigurationException("Please provide executable paths for each Command-Line Interface.");
            }
        }
    }
}
