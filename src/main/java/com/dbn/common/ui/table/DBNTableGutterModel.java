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

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.ui.util.Listeners;
import org.jetbrains.annotations.NotNull;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class DBNTableGutterModel<T extends DBNTableWithGutterModel> extends StatefulDisposableBase implements ListModel {
    private final WeakRef<T> tableModel;
    private final Listeners<ListDataListener> listeners = Listeners.create(this);

    public DBNTableGutterModel(@NotNull T tableModel) {
        this.tableModel = WeakRef.of(tableModel);

        Disposer.register(tableModel, this);
    }

    @NotNull
    public T getTableModel() {
        return tableModel.ensure();
    }

    @Override
    public int getSize() {
        return getTableModel().getRowCount();
    }

    @Override
    public Object getElementAt(int index) {
        return index + 1;
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listeners.remove(l);
    }

    public void notifyListeners(ListDataEvent e) {
        listeners.notify(l -> l.contentsChanged(e));
    }

    @Override
    public void disposeInner() {
        nullify();
    }
}
