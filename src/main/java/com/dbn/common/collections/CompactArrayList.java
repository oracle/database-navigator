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

package com.dbn.common.collections;

import com.dbn.common.util.Unsafe;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import static com.dbn.common.exception.Exceptions.unsupported;

public final class CompactArrayList<T> implements List<T>, RandomAccess, Serializable {
    private Object[] elements;

    public CompactArrayList(List<T> elements) {
        this.elements = elements.toArray();
    }

    public CompactArrayList(Object[] elements) {
        this.elements = elements;
    }

    public CompactArrayList(int size) {
        this.elements = new Object[size];
    }

    public static <T> List<T> from(List<T> list) {
        return new CompactArrayList<>(list);
    }

    @Override
    public int size() {
        return elements.length;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return indexOf(o) >= 0;
    }

    @NotNull
    @Override
    public Iterator<T> iterator() {
        return Unsafe.cast(Arrays.stream(elements).iterator());
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return elements;
    }

    @NotNull
    @Override
    public <E> E[] toArray(@NotNull E[] s) {
        return Unsafe.cast(Arrays.copyOf(this.elements, elements.length, s.getClass()));
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> collection) {
        return false;
    }


    @Override
    public T get(int i) {
        return Unsafe.cast(elements[i]);
    }

    @Override
    public int indexOf(Object o) {
        if (o == null) {
            for(int i = 0; i < elements.length; i++) {
                if (elements[i] == null) {
                    return i;
                }
            }
        } else {
            for(int i = 0; i < elements.length; i++) {
                if (o.equals(elements[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public int lastIndexOf(Object o) {
        int start = 0;
        int i;
        if (o == null) {
            for (i = size() - 1; i >= 0; i--) {
                if (elements[i] == null) {
                    return i;
                }
            }
        } else {
            for(i = size() - 1; i >= 0; i--) {
                if (o.equals(elements[i])) {
                    return i;
                }
            }
        }

        return -1;
    }

    @Override
    public T set(int i, T t) {
        elements[i] = t;
        return t;
    }

    @Override
    public void clear() {
        elements = new Object[0];
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator() {
        // TODO support
        return unsupported();
    }

    @NotNull
    @Override
    public ListIterator<T> listIterator(int i) {
        // TODO support
        return unsupported();
    }

    @NotNull
    @Override
    public List<T> subList(int from, int to) {
        return Unsafe.cast(Arrays.asList(Arrays.copyOfRange(elements, from, to)));
    }

    @Override
    public void sort(Comparator c) {
        Arrays.sort(elements, c);
    }

    /*************************************************************
     *           unsupported update operations                   *
     *************************************************************/

    @Override
    public boolean add(T t) {
        return unsupported();
    }

    @Override
    public boolean remove(Object o) {
        return unsupported();
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends T> collection) {
        return unsupported();
    }

    @Override
    public boolean addAll(int i, @NotNull Collection<? extends T> collection) {
        return unsupported();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> collection) {
        return unsupported();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> collection) {
        return unsupported();
    }

    @Override
    public void add(int i, T t) {
        unsupported();
    }

    @Override
    public T remove(int i) {
        return unsupported();
    }
}
