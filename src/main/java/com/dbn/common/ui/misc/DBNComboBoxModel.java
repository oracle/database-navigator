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

package com.dbn.common.ui.misc;

import com.dbn.common.ui.util.Listeners;
import lombok.Getter;
import lombok.Setter;

import javax.swing.MutableComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
public class DBNComboBoxModel<T> implements MutableComboBoxModel<T> {
    private final Listeners<ListDataListener> listDataListeners = Listeners.create();
    private final List<T> items;
    private T selectedItem;

    public DBNComboBoxModel(Collection<T> items) {
        this.items = new ArrayList<>(items);
    }

    public DBNComboBoxModel() {
        this.items = new ArrayList<>();
    }

    @Override
    public void addElement(T item) {
        int index = items.size();
        items.add(item);
        fireIntervalAdded(index, index);
    }

    @Override
    public void removeElement(Object obj) {
        int index = items.indexOf(obj);
        if (index < 0) return;

        items.remove(index);
        fireIntervalRemoved(index, index);
    }

    @Override
    public void insertElementAt(T item, int index) {
        items.add(index, item);
        fireIntervalAdded(index, index);
    }

    @Override
    public void removeElementAt(int index) {
        items.remove(index);
        fireIntervalRemoved(index, index);
    }

    public void removeAllElements() {
        int size = items.size();
        items.clear();
        if (size > 0) fireIntervalRemoved(0, size - 1);
    }

    @Override
    public void setSelectedItem(Object selectedItem) {
        this.selectedItem = (T) selectedItem;
        fireContentsChanged(-1, -1);
    }

    @Override
    public Object getSelectedItem() {
        return selectedItem;
    }

    @Override
    public int getSize() {
        return items.size();
    }

    @Override
    public T getElementAt(int index) {
        return items.get(index);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listDataListeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listDataListeners.remove(l);
    }

    public boolean containsItem(T item) {
        return items.contains(item);
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    private void fireContentsChanged(int index0, int index1) {
        ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index0, index1);
        listDataListeners.notify(listener -> listener.contentsChanged(event));
    }

    private void fireIntervalAdded(int index0, int index1) {
        ListDataEvent event = new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index0, index1);
        listDataListeners.notify(listener -> listener.intervalAdded(event));
    }

    private void fireIntervalRemoved(int index0, int index1) {
        ListDataEvent event = new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index0, index1);
        listDataListeners.notify(listener -> listener.intervalRemoved(event));
    }
}
