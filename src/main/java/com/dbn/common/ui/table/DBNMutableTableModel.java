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

package com.dbn.common.ui.table;

import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.ui.util.Listeners;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

/**
 * Mutable model, not really editable
 * @param <R>
 */
public abstract class DBNMutableTableModel<R> extends StatefulDisposableBase implements DBNTableModel<R> {
    private final Listeners<TableModelListener> listeners = Listeners.create(this);
    public boolean isReadonly(){
        return true;
    }

    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    public final void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        throw new UnsupportedOperationException();
    }

    public final void addTableModelListener(TableModelListener l) {
        listeners.add(l);
    }

    public final void removeTableModelListener(TableModelListener l) {
        listeners.remove(l);
    }

    public final void notifyRowChange(int row) {
        TableModelEvent event = new TableModelEvent(this, row);
        listeners.notify(l -> l.tableChanged(event));
    }

    public final void notifyRowChanges() {
        TableModelEvent event = new TableModelEvent(this);
        listeners.notify(l -> l.tableChanged(event));
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
