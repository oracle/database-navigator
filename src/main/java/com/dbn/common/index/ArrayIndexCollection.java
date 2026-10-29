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

package com.dbn.common.index;

import java.util.Arrays;

final class ArrayIndexCollection implements IndexCollection {
    private static final int[] EMPTY_ARRAY = new int[0];
    private int[] values;

    ArrayIndexCollection() {
        values = EMPTY_ARRAY;
    }

    ArrayIndexCollection(int... values) {
        this.values = Arrays.copyOf(values, values.length);
    }

    static ArrayIndexCollection fromSortedArray(int[] values) {
        ArrayIndexCollection indexCollection = new ArrayIndexCollection();
        indexCollection.values = values;
        return indexCollection;
    }

    @Override
    public boolean isEmpty() {
        return values.length == 0;
    }

    @Override
    public synchronized void add(int value) {
        insert(value);
    }

    @Override
    public synchronized boolean addIfAbsent(int value) {
        return insert(value);
    }

    private boolean insert(int value) {
        int index = insertionIndex(value);
        if (index == -1) return false;

        int[] copy = new int[values.length + 1];
        copy[index] = value;
        System.arraycopy(values, 0, copy, 0, index);
        System.arraycopy(values, index, copy, index + 1, values.length - index);
        values = copy;
        return true;
    }

    private int insertionIndex(int value) {
        if (values.length == 0) return 0;

        int left = 0;
        int right = values.length - 1;
        if (value < values[left]) return 0;
        if (value > values[right]) return values.length;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (values[mid] == value) return -1;
            if (values[mid] < value) left = mid + 1; else right = mid - 1;
        }

        return left;
    }

    @Override
    public boolean contains(int value) {
        if (values.length == 0) return false;
        int left = 0;
        int right = values.length - 1;
        if (value < values[left] || value > values[right]) return false;

        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (values[mid] == value) return true;
            if (values[mid] > value) right = mid - 1; else left = mid + 1;
        }
        return false;
    }

    @Override
    public int indexOf(int value) {
        if (values.length == 0 || value < values[0] || value > values[values.length - 1]) return -1;

        int left = 0;
        int right = values.length - 1;
        while (left <= right) {
            int mid = left + (right - left) / 2;
            if (values[mid] == value) return mid;
            if (values[mid] > value) right = mid - 1; else left = mid + 1;
        }
        return -1;
    }

    @Override
    public int size() {
        return values.length;
    }

    @Override
    public int[] values() {
        return values;
    }

    @Override
    public String toString() {
        return Arrays.toString(values);
    }
}
