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
        switch (columnIndex) {
            case 0: return "Name";
            case 1: return "Protocol";
            case 2: return "Host";
            case 3: return "Port";
            case 4: return "SID";
            case 5: return "Service Name";
            case 6: return "Global Name";
            case 7: return "Failover";
            case 8: return "Type";
            case 9: return "Method";
            default: return "";
        }
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
        switch (column) {
            case 0: return tnsProfile.getProfile();
            case 1: return tnsProfile.getProtocol();
            case 2: return tnsProfile.getHost();
            case 3: return tnsProfile.getPort();
            case 4: return tnsProfile.getSid();
            case 5: return tnsProfile.getServiceName();
            case 6: return tnsProfile.getGlobalName();
            case 7: return tnsProfile.getFailover();
            case 8: return tnsProfile.getFailoverType();
            case 9: return tnsProfile.getFailoverMethod();
            default: return "";
        }
    }

    @Override
    public String getPresentableValue(TnsProfile tnsProfile, int column) {
        if (tnsProfile == null) return "";

        switch (column) {
            case 0: return tnsProfile.getProfile();
            case 1: return tnsProfile.getProtocol();
            case 2: return tnsProfile.getHost();
            case 3: return tnsProfile.getPort();
            case 4: return tnsProfile.getSid();
            case 5: return tnsProfile.getServiceName();
            case 6: return tnsProfile.getGlobalName();
            case 7: return tnsProfile.getFailover();
            case 8: return tnsProfile.getFailoverType();
            case 9: return tnsProfile.getFailoverMethod();
            default: return "";
        }
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
