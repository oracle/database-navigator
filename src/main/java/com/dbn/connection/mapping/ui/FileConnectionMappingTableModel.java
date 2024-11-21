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

package com.dbn.connection.mapping.ui;

import com.dbn.common.ui.table.DBNMutableTableModel;
import com.dbn.common.util.Safe;
import com.dbn.connection.mapping.FileConnectionContext;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public class FileConnectionMappingTableModel extends DBNMutableTableModel<FileConnectionContext> {
    public static final String[] COLUMNS = {"File", "Connection", "Environment", "Schema", "Session", ""};
    private final List<FileConnectionContext> mappings;

    public FileConnectionMappingTableModel(List<FileConnectionContext> mappings) {
        this.mappings = mappings;
    }

    public final int indexOf(@NotNull VirtualFile file) {
        for (int i = 0; i < mappings.size(); i++) {
            FileConnectionContext mapping = mappings.get(i);
            if (Objects.equals(mapping.getFile(), file)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public final int getRowCount() {
        return mappings.size();
    }

    @Override
    public final int getColumnCount() {
        return COLUMNS.length;
    }

    @Override
    public final String getColumnName(int columnIndex) {
        return COLUMNS[columnIndex];
    }

    @Override
    public final Class<?> getColumnClass(int columnIndex) {
        return FileConnectionContext.class;
    }

    @Override
    public final Object getValueAt(int rowIndex, int columnIndex) {
        return mappings.get(rowIndex);
    }

    @Override
    public Object getValue(FileConnectionContext row, int column) {
        switch (column) {
            case 0: return row.getFile();
            case 1: return row.getConnection();
            case 2: return Safe.call(row.getConnection(), c -> c.getEnvironmentType());
            case 3: return row.getSchemaId();
            case 4: return row.getSession();
        }
        return "";
    }

    @Override
    public String getPresentableValue(FileConnectionContext row, int column) {
        switch (column) {
            case 0: return Safe.call(row.getFile(), f -> f.getPath(), "");
            case 1: return Safe.call(row.getConnection(), c -> c.getName(), "");
            case 2: return Safe.call(row.getConnection(), c -> c.getEnvironmentType().getName(), "");
            case 3: return Safe.call(row.getSchemaId(), s -> s.getName(), "");
            case 4: return Safe.call(row.getSession(), s -> s.getName(), "");
        }
        return "";
    }

    @Override
    public void disposeInner() {
        super.disposeInner();
        mappings.clear();
    }
}
