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

package com.dbn.common.list;

import com.dbn.common.collections.CompactArrayList;
import com.dbn.common.filter.Filter;
import com.dbn.common.latent.Latent;
import com.dbn.common.util.Compactables;
import com.dbn.common.util.Lists;
import com.dbn.common.util.Unsafe;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;

final class StatefulFilteredList<T> extends FilteredListBase<T> {
    private final Latent<List<T>> inner = Latent.mutable(
            () -> filterSignature(),
            () -> createInner());

    @Nullable
    private List<T> createInner() {
        if (filter == null || filter.isEmpty()) {
            return null;
        }

        List<T> filtered = Unsafe.cast(Lists.filtered(base, t -> filter.accepts(t)));
        return CompactArrayList.from(filtered);
    }


    StatefulFilteredList(Filter<T> filter, List<T> base) {
        super(filter, base);
    }

    StatefulFilteredList(Filter<T> filter) {
        super(filter);
    }

    @Override
    List<T> initBase(List<T> source) {
        return source == null ? new ArrayList<>() : source;
    }

    @Override
    public List<T> getBase() {return base;}

    // update methods should not be affected by filtering
    @Override
    public void sort(Comparator<? super T> comparator) {
        stateAware(() -> base.sort(comparator));
    }

    @Override
    public boolean add(T o) {
        return stateAware(() -> base.add(o));
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        return stateAware(() -> base.addAll(c));
    }

    @Override
    public boolean remove(Object o) {
        return stateAware(() -> base.remove(o));
    }

    @Override
    public boolean removeAll(@NotNull Collection c) {
        return stateAware(() -> base.removeAll(c));
    }

    @Override
    public boolean retainAll(@NotNull Collection c) {
        return stateAware(() -> base.retainAll(c));
    }

    @Override
    public void clear() {
        if (!base.isEmpty()) {
            stateAware(() -> base.clear());
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    private boolean isFiltered() {
        return getFilter() != null;
    }

    @Override
    public int size() {
        return list().size();
    }

    @Override
    @NotNull
    public Iterator<T> iterator(){
        return list().iterator();
    }

    @Override
    @NotNull
    public Object[] toArray() {
        return list().toArray();
    }

    @Override
    @NotNull
    public <E> E[] toArray(@NotNull E[] e) {
        return list().toArray(e);
    }

    @Override
    public boolean contains(Object o){
        return list().contains(o);
    }

    @Override
    public int indexOf(Object o) {
        return list().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list().lastIndexOf(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection c) {
        return list().containsAll(c);
    }

    public boolean equals(Object o){
        return list().equals(o);
    }

    private int findIndex(int index) {
        int count = -1;
        Filter<T> filter = getFilter();
        for (int i = 0; i < base.size(); i++) {
            T object = base.get(i);
            if (filter == null || filter.accepts(object)) count++;
            if (count == index) return i;
        }
        return -1;
    }

    @Override
    public void add(int index, T element){
        stateAware(() -> {
            if (isFiltered()) {
                int idx = findIndex(index);
                base.add(idx, element);
            } else {
                base.add(index, element);
            }
        });
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends T> c){
        return stateAware(() -> {
            if (isFiltered()) {
                int idx = findIndex(index);
                return base.addAll(idx, c);
            } else {
                return base.addAll(index, c);
            }
        });
    }

    @Override
    public T get(int index){
        return list().get(index);
    }

    @Override
    public T set(int index, T element) {
        return stateAware(() -> {
            if (isFiltered()) {
                int idx = findIndex(index);
                base.set(idx, element);
                return base.get(idx);
            } else {
                return base.set(index, element);
            }
        });
    }

    @Override
    public T remove(int index) {
        return stateAware(() -> {
            if (isFiltered()) {
                int idx = findIndex(index);
                return base.remove(idx);
            } else {
                return base.remove(index);
            }
        });
    }

    @Override
    @NotNull
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException("List iterator not implemented in filtrable actions");
    }

    @Override
    @NotNull
    public ListIterator<T> listIterator(int index) {
        throw new UnsupportedOperationException("List iterator not implemented in filtrable actions");
    }

    @Override
    @NotNull
    public List<T> subList(int fromIndex, int toIndex) {
        throw new UnsupportedOperationException("Sublist not implemented in filtrable actions");
    }

    @Override
    public void trimToSize() {
        stateAware(() -> base = Compactables.compact(base));
    }

    @Override
    public void setFilter(Filter<T> filter) {
        stateAware(() -> super.setFilter(filter));
    }

    private List<T> list() {
        List<T> filtered = inner.get();
        return filtered == null ? base : filtered;
    }

    private int filterSignature(){
        Filter<T> filter = getFilter();
        return filter == null ? 0 : filter.getSignature();
    }

    @SneakyThrows
    private <R> R stateAware(Callable<R> action) {
        try {
            return action.call();
        } finally {
            inner.reset();
        }
    }

    @SneakyThrows
    private void stateAware(Runnable action) {
        try {
            action.run();
        } finally {
            inner.reset();
        }
    }
}
