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

package com.dbn.connection.config.file.ui;

import com.dbn.common.ui.table.DBNEditableTableModel;
import com.dbn.common.util.Commons;
import com.dbn.connection.config.file.DatabaseFile;
import com.dbn.connection.config.file.DatabaseFileBundle;
import lombok.Getter;

import javax.swing.event.TableModelListener;

@Getter
public class DatabaseFilesTableModel extends DBNEditableTableModel {
    private DatabaseFileBundle fileBundle;

    DatabaseFilesTableModel(DatabaseFileBundle fileBundle) {
        setFileBundle(fileBundle);
        addTableModelListener(defaultModelListener);
    }

    public void setFileBundle(DatabaseFileBundle fileBundle) {
        this.fileBundle = fileBundle == null ? new DatabaseFileBundle() : fileBundle.clone();
    }

    @Override
    public int getRowCount() {
        return fileBundle.size();
    }
    
    TableModelListener defaultModelListener = e -> {};

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnIndex == 0 ? "File Path" :
               columnIndex == 1 ? "Database Name" : null;
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return true; //!(rowIndex == 0 && columnIndex == 1);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        DatabaseFile filePathOption = fileBundle.get(rowIndex);
        return
           columnIndex == 0 ? filePathOption.getPath() :
           columnIndex == 1 ? filePathOption.getSchema() : null;
    }

    @Override
    public void setValueAt(Object o, int rowIndex, int columnIndex) {
        Object actualValue = getValueAt(rowIndex, columnIndex);
        if (!Commons.match(actualValue, o)) {
            DatabaseFile databaseFile = fileBundle.get(rowIndex);
            if (columnIndex == 0) {
                databaseFile.setPath((String) o);
                fileBundle.adjustSchemaName(databaseFile);
            } else if (columnIndex == 1) {
                databaseFile.setSchema((String) o);
            }

            notifyListeners(rowIndex, rowIndex, columnIndex);
        }
    }

    private DatabaseFile getFile(int rowIndex) {
        while (fileBundle.size() <= rowIndex) {
            fileBundle.add(new DatabaseFile());
        }
        return fileBundle.get(rowIndex);
    }

    @Override
    public void insertRow(int rowIndex) {
        fileBundle.add(rowIndex, new DatabaseFile());
        notifyListeners(rowIndex, fileBundle.size()-1, -1);
    }

    @Override
    public void removeRow(int rowIndex) {
        if (fileBundle.size() > rowIndex) {
            fileBundle.remove(rowIndex);
            notifyListeners(rowIndex, fileBundle.size()-1, -1);
        }
    }

    public void validate() {

    }
}
