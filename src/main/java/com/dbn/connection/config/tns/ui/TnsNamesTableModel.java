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

package com.dbn.connection.config.tns.ui;

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.ui.table.DBNReadonlyTableModel;
import com.dbn.common.ui.util.Listeners;
import com.dbn.connection.config.tns.TnsNames;
import com.dbn.connection.config.tns.TnsProfile;
import org.jetbrains.annotations.NonNls;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.List;

public class TnsNamesTableModel extends StatefulDisposableBase implements DBNReadonlyTableModel<TnsProfile> {
    private final TnsNames tnsNames;
    private final Listeners<TableModelListener> listeners = Listeners.create(this);

    TnsNamesTableModel(TnsNames tnsNames) {
        super();
        this.tnsNames = tnsNames;
    }


    @Override
    public int getRowCount() {
        return tnsNames.size();
    }

    @Override
    public int getColumnCount() {
        return 10;
    }

    @NonNls
    @Override
    public String getColumnName(int columnIndex) {
        return switch (columnIndex) {
            case 0 -> "Name";
            case 1 -> "Protocol";
            case 2 -> "Host";
            case 3 -> "Port";
            case 4 -> "SID";
            case 5 -> "Service Name";
            case 6 -> "Global Name";
            case 7 -> "Failover";
            case 8 -> "Type";
            case 9 -> "Method";
            default -> "";
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return TnsProfile.class;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return tnsNames.getProfiles().get(rowIndex);
        //return getColumnValue(tnsName, columnIndex);
    }

    @Override
    public Object getValue(TnsProfile tnsProfile, int column) {
        return switch (column) {
            case 0 -> tnsProfile.getProfile();
            case 1 -> tnsProfile.getProtocol();
            case 2 -> tnsProfile.getHost();
            case 3 -> tnsProfile.getPort();
            case 4 -> tnsProfile.getSid();
            case 5 -> tnsProfile.getServiceName();
            case 6 -> tnsProfile.getGlobalName();
            case 7 -> tnsProfile.getFailover();
            case 8 -> tnsProfile.getFailoverType();
            case 9 -> tnsProfile.getFailoverMethod();
            default -> "";
        };
    }

    @Override
    public String getPresentableValue(TnsProfile tnsProfile, int column) {
        if (tnsProfile == null) return "";

        return switch (column) {
            case 0 -> tnsProfile.getProfile();
            case 1 -> tnsProfile.getProtocol();
            case 2 -> tnsProfile.getHost();
            case 3 -> tnsProfile.getPort();
            case 4 -> tnsProfile.getSid();
            case 5 -> tnsProfile.getServiceName();
            case 6 -> tnsProfile.getGlobalName();
            case 7 -> tnsProfile.getFailover();
            case 8 -> tnsProfile.getFailoverType();
            case 9 -> tnsProfile.getFailoverMethod();
            default -> "";
        };
    }

    public void filter(String text) {
        boolean changed = tnsNames.getFilter().setText(text);
        if (changed) {
            TableModelEvent modelEvent = new TableModelEvent(this);
            listeners.notify(l -> l.tableChanged(modelEvent));
        }
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }


    public List<TnsProfile> getProfiles() {
        return tnsNames.getProfiles();
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
