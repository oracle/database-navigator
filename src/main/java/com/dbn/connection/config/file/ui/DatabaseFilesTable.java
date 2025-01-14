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

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.table.DBNEditableTable;
import com.dbn.common.ui.table.FileBrowserTableCellEditor;
import com.dbn.connection.config.file.DatabaseFileBundle;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class DatabaseFilesTable extends DBNEditableTable<DatabaseFilesTableModel> {

    public DatabaseFilesTable(@NotNull DBNComponent parent, DatabaseFileBundle databaseFiles) {
        super(parent, new DatabaseFilesTableModel(databaseFiles), false);
        setDefaultRenderer(Object.class, new DatabaseFilesTableCellRenderer());
        FileChooserDescriptor fileChooserDescriptor = new FileChooserDescriptor(true, true, false, false, false, false);
        FileBrowserTableCellEditor fileChooser = new FileBrowserTableCellEditor(fileChooserDescriptor);
        getColumnModel().getColumn(0).setCellEditor(fileChooser);
        setFixedColumnWidth(1, 120);

        setAccessibleName(this, "Database Files");
    }

    public void setFilePaths(DatabaseFileBundle filesBundle) {
        super.setModel(new DatabaseFilesTableModel(filesBundle));
    }

}
