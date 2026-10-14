/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.driver.dashboard;

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.ui.table.DBNReadonlyTableModel;
import com.dbn.common.ui.util.Listeners;
import com.dbn.driver.download.metadata.DriverPackage;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.util.Strings.truncateWithMiddleEllipsis;
import static com.dbn.nls.NlsResources.txt;

class DriverLibraryDashboardTableModel extends StatefulDisposableBase implements DBNReadonlyTableModel<DriverLibraryDashboardTableModel.Row> {
    private static final int LOCATION_DISPLAY_MAX_LENGTH = 60;

    private final Listeners<TableModelListener> listeners = Listeners.create(this);
    private final List<Row> rows = new ArrayList<>();

    DriverLibraryDashboardTableModel() {
        super();
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> txt("app.driver.column.DatabaseType");
            case 1 -> txt("app.driver.column.DriverPackage");
            case 2 -> txt("app.driver.column.Status");
            case 3 -> txt("app.driver.column.Location");
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return Row.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex);
    }

    @Override
    public Object getValue(Row row, int column) {
        return getPresentableValue(row, column);
    }

    @Override
    public String getPresentableValue(Row row, int column) {
        if (row == null) return "";
        return switch (column) {
            case 0 -> row.driverPackage().getDatabaseType() == null ? "" : row.driverPackage().getDatabaseType().getName();
            case 1 -> row.driverPackage().getName() == null ? row.driverPackage().getId() : row.driverPackage().getName();
            case 2 -> txt("app.driver.const.DriverStatus_" + row.status().name());
            case 3 -> truncateWithMiddleEllipsis(row.location(), LOCATION_DISPLAY_MAX_LENGTH);
            default -> "";
        };
    }

    @Override
    public String getTooltip(Row row, int column) {
        if (row == null) return null;
        if (column == 2 && row.errorMessage() != null) return row.errorMessage();
        if (column == 3) return row.location();
        return null;
    }

    void setRows(List<Row> newRows) {
        rows.clear();
        rows.addAll(newRows);
        fireTableChanged(new TableModelEvent(this));
    }

    @Nullable
    Row getRow(int rowIndex) {
        return rowIndex < 0 || rowIndex >= rows.size() ? null : rows.get(rowIndex);
    }

    @Override
    public void addTableModelListener(TableModelListener listener) {
        listeners.add(listener);
    }

    @Override
    public void removeTableModelListener(TableModelListener listener) {
        listeners.remove(listener);
    }

    private void fireTableChanged(TableModelEvent event) {
        listeners.notify(listener -> listener.tableChanged(event));
    }

    @Override
    public void disposeInner() {
        rows.clear();
        nullify();
    }

    record Row(
            DriverPackage driverPackage,
            DriverLibraryDashboardStatus status,
            String location,
            @Nullable String errorMessage) {}
}
