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

package com.dbn.common.index;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static java.util.Collections.emptySet;

public class StagingIndexContainer<T extends Indexable> extends BackedIndexContainer<T> {
    // Mutations are valid only before freeze() or discard(); afterwards the container is read-only.
    private transient StagingIndex stagingIndex = new StagingIndex();

    public StagingIndexContainer(IndexResolver<T> resolver) {
        super(resolver);
    }

    @Override
    public void add(T element) {
        stagingIndex.add(element.index());
    }

    public void add(int index) {
        stagingIndex.add(index);
    }

    public boolean addIfAbsent(T element) {
        return stagingIndex.addIfAbsent(element.index());
    }

    public boolean addIfAbsent(int index) {
        return stagingIndex.addIfAbsent(index);
    }

    @Override
    public void addAll(Collection<T> elements) {
        for (T element : elements) {
            stagingIndex.add(element.index());
        }
    }

    @Override
    public int size() {
        StagingIndex stagingIndex = this.stagingIndex;
        return stagingIndex == null ? super.size() : stagingIndex.size();
    }

    @Override
    public boolean isEmpty() {
        StagingIndex stagingIndex = this.stagingIndex;
        return stagingIndex == null ? super.isEmpty() : stagingIndex.isEmpty();
    }

    @Override
    public boolean contains(T element) {
        return contains(element.index());
    }

    public boolean contains(int index) {
        StagingIndex stagingIndex = this.stagingIndex;
        if (stagingIndex == null) return indices.contains(index);

        if (index < 0) throw new IndexOutOfBoundsException("Negative index: " + index);
        int wordIndex = index >>> 6;
        return wordIndex < stagingIndex.words.length &&
                (stagingIndex.words[wordIndex] & (1L << index)) != 0;
    }

    @Override
    public synchronized Set<T> elements() {
        StagingIndex stagingIndex = this.stagingIndex;
        if (stagingIndex != null) {
            int[] values = stagingIndex.toSortedArray();
            return values.length == 0 ? emptySet() : buildElements(values, resolver);
        }

        return super.elements();
    }

    public synchronized void freeze() {
        if (stagingIndex == null) return;

        replace(stagingIndex.toSortedArray());
        stagingIndex = null;
    }

    public synchronized void discard() {
        stagingIndex = null;
    }

    private static final class StagingIndex {
        private long[] words = new long[1];
        private int size;

        private void add(int index) {
            if (index < 0) throw new IndexOutOfBoundsException("Negative index: " + index);
            int wordIndex = index >>> 6;
            long[] words = this.words;
            if (wordIndex >= words.length) {
                int capacity = Math.max(wordIndex + 1, words.length << 1);
                words = Arrays.copyOf(words, capacity);
                this.words = words;
            }

            long bit = 1L << index;
            long word = words[wordIndex];
            if ((word & bit) == 0) {
                words[wordIndex] = word | bit;
                size++;
            }
        }

        private boolean addIfAbsent(int index) {
            if (index < 0) throw new IndexOutOfBoundsException("Negative index: " + index);
            int wordIndex = index >>> 6;
            long[] words = this.words;
            if (wordIndex >= words.length) {
                int capacity = Math.max(wordIndex + 1, words.length << 1);
                words = Arrays.copyOf(words, capacity);
                this.words = words;
            }

            long bit = 1L << index;
            long word = words[wordIndex];
            if ((word & bit) != 0) return false;

            words[wordIndex] = word | bit;
            size++;
            return true;
        }

        private int size() {
            return size;
        }

        private boolean isEmpty() {
            return size == 0;
        }

        private int[] toSortedArray() {
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
    }
}
