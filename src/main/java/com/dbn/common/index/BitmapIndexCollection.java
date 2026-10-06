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

public final class BitmapIndexCollection implements IndexCollection {
    private static final int[] EMPTY_ARRAY = new int[0];

    private long[] words = new long[1];
    private int size;
    private int maxIndex = -1;

    public BitmapIndexCollection() {
    }

    private BitmapIndexCollection(long[] words, int size, int maxIndex) {
        this.words = words;
        this.size = size;
        this.maxIndex = maxIndex;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void add(int value) {
        if (value < 0) throw new IndexOutOfBoundsException("Negative index: " + value);
        int wordIndex = value >>> 6;
        long[] words = this.words;
        if (wordIndex >= words.length) {
            int capacity = Math.max(wordIndex + 1, words.length << 1);
            words = Arrays.copyOf(words, capacity);
            this.words = words;
        }

        long bit = 1L << value;
        long word = words[wordIndex];
        if ((word & bit) == 0) {
            words[wordIndex] = word | bit;
            size++;
            if (value > maxIndex) maxIndex = value;
        }
    }

    @Override
    public boolean addIfAbsent(int value) {
        if (value < 0) throw new IndexOutOfBoundsException("Negative index: " + value);
        int wordIndex = value >>> 6;
        long[] words = this.words;
        if (wordIndex >= words.length) {
            int capacity = Math.max(wordIndex + 1, words.length << 1);
            words = Arrays.copyOf(words, capacity);
            this.words = words;
        }

        long bit = 1L << value;
        long word = words[wordIndex];
        if ((word & bit) != 0) return false;

        words[wordIndex] = word | bit;
        size++;
        if (value > maxIndex) maxIndex = value;
        return true;
    }

    @Override
    public boolean contains(int value) {
        if (value < 0) return false;
        int wordIndex = value >>> 6;
        return wordIndex < words.length && (words[wordIndex] & (1L << value)) != 0;
    }

    @Override
    public int indexOf(int value) {
        if (value < 0) return -1;
        int[] values = values();
        int index = Arrays.binarySearch(values, value);
        return index < 0 ? -1 : index;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int[] values() {
        return toSortedArray();
    }

    IndexCollection freeze() {
        if (size == 0) return new ArrayIndexCollection();

        int wordCount = (maxIndex >>> 6) + 1;
        if (size > wordCount * 2L) {
            return new BitmapIndexCollection(Arrays.copyOf(words, wordCount), size, maxIndex);
        }

        return ArrayIndexCollection.fromSortedArray(toSortedArray());
    }

    private int[] toSortedArray() {
        if (size == 0) return EMPTY_ARRAY;

        int[] values = new int[size];
        int offset = 0;
        for (int wordIndex = 0; wordIndex < words.length; wordIndex++) {
            long word = words[wordIndex];
            while (word != 0) {
                int bitIndex = Long.numberOfTrailingZeros(word);
                values[offset++] = (wordIndex << 6) + bitIndex;
                word &= word - 1;
            }
        }
        return values;
    }

    @Override
    public String toString() {
        return Arrays.toString(values());
    }
}
