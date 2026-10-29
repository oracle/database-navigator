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
import com.intellij.openapi.util.NlsContexts.Tooltip;
import lombok.experimental.Delegate;
import org.jetbrains.annotations.Nullable;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static com.dbn.nls.NlsResources.txt;

class DriverLibraryDashboardTableModel extends StatefulDisposableBase implements DBNReadonlyTableModel<DriverLibraryDashboardTableModel.DashboardDriverPackage> {
    private static final int LOCATION_DISPLAY_MAX_LENGTH = 60;

    private final Listeners<TableModelListener> listeners = Listeners.create(this);
    private final List<DashboardDriverPackage> driverPackages = new ArrayList<>();
    private final Function<DashboardDriverPackage, @Nullable @Tooltip String> tooltipProvider;

    DriverLibraryDashboardTableModel(Function<DashboardDriverPackage, @Nullable @Tooltip String> tooltipProvider) {
        super();
        this.tooltipProvider = tooltipProvider;
    }

    @Override
    public int getRowCount() {
        return driverPackages.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> txt("app.driver.column.DatabaseType");
            case 1 -> txt("app.driver.column.DriverPackage");
            case 2 -> txt("app.driver.column.Status");
            case 3 -> txt("app.driver.column.DownloadLocation");
            case 4 -> "";
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return DashboardDriverPackage.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return driverPackages.get(rowIndex);
    }

    @Override
    public Object getValue(DashboardDriverPackage driverPackage, int column) {
        return getPresentableValue(driverPackage, column);
    }

    @Override
    public String getPresentableValue(DashboardDriverPackage driverPackage, int column) {
        if (driverPackage == null) return "";
        return switch (column) {
            case 0 -> driverPackage.getDatabaseType() == null ? "" : driverPackage.getDatabaseType().getName();
            case 1 -> driverPackage.getName() == null ? driverPackage.getId() : driverPackage.getName();
            case 2 -> getStatusText(driverPackage);
            case 3 -> driverPackage.downloaded() ? truncatePathFromStart(driverPackage.location()) : "";
            case 4 -> "";
            default -> "";
        };
    }

    private static String getStatusText(DashboardDriverPackage driverPackage) {
        if (!driverPackage.supported() && driverPackage.status() == DriverLibraryDashboardStatus.AVAILABLE) {
            return txt("app.driver.const.DriverStatus_NOT_SUPPORTED");
        }
        return txt("app.driver.const.DriverStatus_" + driverPackage.status().name());
    }

    @Override
    public @Nullable @Tooltip String getTooltip(DashboardDriverPackage driverPackage, int column) {
        if (driverPackage == null) return null;
        if (column == 2 && driverPackage.errorMessage() != null) return driverPackage.errorMessage();
        if (column == 3 && driverPackage.downloaded()) return driverPackage.location();
        if (column == 4) return tooltipProvider.apply(driverPackage);
        return null;
    }

    void setRows(List<DashboardDriverPackage> newDriverPackages) {
        driverPackages.clear();
        driverPackages.addAll(newDriverPackages);
        TableModelEvent event = new TableModelEvent(
                this,
                0,
                Integer.MAX_VALUE,
                TableModelEvent.ALL_COLUMNS,
                TableModelEvent.UPDATE);
        fireTableChanged(event);
    }

    @Nullable
    DriverLibraryDashboardTableModel.DashboardDriverPackage getRow(int rowIndex) {
        return rowIndex < 0 || rowIndex >= driverPackages.size() ? null : driverPackages.get(rowIndex);
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

    private static String truncatePathFromStart(String path) {
        if (path == null || path.length() <= LOCATION_DISPLAY_MAX_LENGTH) return path;
        int suffixLength = LOCATION_DISPLAY_MAX_LENGTH - 3;
        return "..." + path.substring(path.length() - suffixLength);
    }

    @Override
    public void disposeInner() {
        driverPackages.clear();
        nullify();
    }

    record DashboardDriverPackage(
            @Delegate DriverPackage driverPackage,
            DriverLibraryDashboardStatus status,
            boolean supported,
            boolean downloaded,
            String location,
            @Nullable String errorMessage) {}
}
