/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.ui.list;

import com.dbn.common.ui.util.Listeners;
import lombok.Getter;

import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static javax.swing.event.ListDataEvent.CONTENTS_CHANGED;

public class MutableObjectListModel<T> implements ListModel<T> {
    private final Listeners<ListDataListener> listeners = Listeners.create();
    private final @Getter List<T> elements;

    public MutableObjectListModel(List<T> elements) {
        this.elements = new ArrayList<>(elements);
    }

    public void add(T element) {
        if (this.elements.contains(element)) return;

        int index = this.elements.size();
        this.elements.add(element);

        ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, index, elements.size());
        this.listeners.notify(l -> l.contentsChanged(event));
    }

    public void addAll(Collection<T> elements) {
        int index = this.elements.size();
        int count = 0;
        for (T element : elements) {
            if (this.elements.contains(element)) continue;

            this.elements.add(element);
            count++;
        }

        ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, index, index + count);
        this.listeners.notify(l -> l.contentsChanged(event));
    }

    public void reset(List<T> elements) {
        this.elements.clear();
        addAll(elements);
    }

    public boolean contains(T element) {
        return this.elements.contains(element);
    }

    @Override
    public int getSize() {
        return elements.size();
    }

    @Override
    public T getElementAt(int index) {
        return elements.get(index);
    }

    public void moveRowsUp(int[] indices) {
        if (indices.length == 0) return;
        if (indices[0] == 0) return;

        for (int index : indices) {
            swap(index, index - 1);
        }

        ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, indices[0] -1, indices[indices.length - 1]);
        listeners.notify(l -> l.contentsChanged(event));

    }

    public void moveRowsDown(int[] indices) {
        if (indices.length == 0) return;
        if (indices[indices.length - 1] >= getSize()) return;

        for (int i = indices.length - 1; i >= 0; i--) {
            swap(indices[i], indices[i] + 1);
        }

        ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, indices[0], indices[indices.length - 1] + 1);
        listeners.notify(l -> l.contentsChanged(event));
    }

    private void swap(int index1, int index2) {
        if (index2 == -1) return;

        T element1 = elements.get(index1);
        T element2 = elements.get(index2);
        elements.set(index2, element1);
        elements.set(index1, element2);
    }

    public void removeRows(int[] indices) {
        for (int i = indices.length - 1; i >= 0; i--) {
            int index = indices[i];
            if (index >= 0 && index < elements.size()) {
                elements.remove(index);
            }
        }

        ListDataEvent event = new ListDataEvent(this, CONTENTS_CHANGED, indices[0], elements.size());
        listeners.notify(l -> l.contentsChanged(event));
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listeners.add(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listeners.remove(l);
    }
}
