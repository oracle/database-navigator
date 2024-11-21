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

import com.dbn.common.filter.Filter;
import com.dbn.common.util.Compactables;
import com.dbn.common.util.Lists;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

public final class StatelessFilteredList<T> extends FilteredListBase<T> {

    StatelessFilteredList(Filter<T> filter, List<T> base) {
        super(filter, base);
    }

    StatelessFilteredList(Filter<T> filter) {
        super(filter);
    }

    @Override
    List<T> initBase(List<T> source) {
        return source == null ?
                new CopyOnWriteArrayList<>() :
                new CopyOnWriteArrayList<>(source);
    }

    @Override
    public List<T> getBase() {return base;}

    // update methods should not be affected by filtering
    @Override
    public void sort(Comparator<? super T> comparator)          {
        base.sort(comparator);}

    @Override
    public boolean add(T o) {
        return base.add(o);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> c) {
        return base.addAll(c);
    }

    @Override
    public boolean remove(Object o) {
        return base.remove(o);
    }

    @Override
    public boolean removeAll(@NotNull Collection c) {
        return base.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection c) {
        return base.retainAll(c);
    }

    @Override
    public void clear() {
        if (!base.isEmpty()) base.clear();
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
        Filter<T> filter = getFilter();
        if (filter != null) {
            return Lists.count(base, e -> filter.accepts(e));
        } else {
            return base.size();

        }
    }

    @Override
    @NotNull
    public Iterator<T> iterator(){
        final Filter<T> filter = getFilter();
        if (filter != null) {
            return new Iterator<>() {
                private final Iterator<T> iterator = base.iterator();
                private T next = findNext();

                private T findNext() {
                    while (iterator.hasNext()) {
                        next = iterator.next();
                        if (filter.accepts(next)) return next;
                    }
                    return null;
                }

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public T next() {
                    T result = next;
                    next = findNext();
                    return result;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException("Iterator remove not implemented in filtrable actions");
                }
            };
        } else {
            return base.iterator();

        }
    }

    @Override
    @NotNull
    public Object[] toArray() {
        Filter<T> filter = getFilter();
        if (filter != null) {
            List<T> result = new ArrayList<>();
            for (T object : base) if (filter.accepts(object)) result.add(object);
            return result.toArray();
        } else {
            return base.toArray();
        }
    }

    @Override
    @NotNull
    public <E> E[] toArray(@NotNull E[] e) {
        Filter<T> filter = getFilter();
        if (filter != null) {
            List<T> result = new ArrayList<>();
            for (T object : base) if (filter.accepts(object)) result.add(object);
            return result.toArray(e);
        } else {
            return base.toArray(e);
        }

    }

    @Override
    public boolean contains(Object o){
        if (isFiltered()) {
            return indexOf(o) > -1;
        } else {
            return base.contains(o);
        }

    }

    @Override
    public int indexOf(Object o) {
        Filter<T> filter = getFilter();
        if (filter != null) {
            if (!filter.accepts((T) o)) return -1;

            int index = 0;
            for (T object : base) {
                if (object.equals(o)) return index;
                if (filter.accepts(object)) index++;
            }
            return -1;
        } else {
            return base.indexOf(o);
        }
    }

    @Override
    public int lastIndexOf(Object o) {
        Filter<T> filter = getFilter();
        if (filter != null) {
            if (!filter.accepts((T) o)) return -1;

            int index = size()-1;
            for (int i = base.size()-1; i > -1; i--) {
                T object = base.get(i);
                if (object.equals(o)) return index;
                if (filter.accepts(object)) index--;
            }
            return -1;
        } else {
            return base.lastIndexOf(o);
        }

    }

    @Override
    public boolean containsAll(@NotNull Collection c) {
        if (isFiltered()) {
            List list = Arrays.asList(toArray());
            return list.containsAll(c);
        } else {
            return base.containsAll(c);
        }
    }

    public boolean equals(Object o){
        if (isFiltered()) {
            List list = Arrays.asList(toArray());
            return list.equals(o);
        } else {
            return base.equals(o);
        }

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
        if (isFiltered()) {
            int idx = findIndex(index);
            base.add(idx, element);
        } else {
            base.add(index, element);
        }
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends T> c){
        if (isFiltered()) {
            int idx = findIndex(index);
            return base.addAll(idx, c);
        } else {
            return base.addAll(index, c);
        }
    }

    @Override
    public T get(int index){
        if (isFiltered()) {
            int idx = findIndex(index);
            return idx == -1 ? null : base.get(idx);
        } else {
            return base.get(index);
        }
    }

    @Override
    public T set(int index, T element) {
        if (isFiltered()) {
            int idx = findIndex(index);
            base.set(idx, element);
            return base.get(idx);
        } else {
            return base.set(index, element);
        }
    }

    @Override
    public T remove(int index) {
        if (isFiltered()) {
            int idx = findIndex(index);
            return base.remove(idx);
        } else {
            return base.remove(index);
        }
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
        base = Compactables.compact(base);
    }
}
