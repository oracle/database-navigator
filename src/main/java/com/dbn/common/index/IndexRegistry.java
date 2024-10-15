package com.dbn.common.index;


import com.intellij.util.containers.IntObjectMap;

import static com.intellij.concurrency.ConcurrentCollectionFactory.createConcurrentIntObjectMap;

public class IndexRegistry<T extends Indexable> {
    private final IntObjectMap<T> INDEX = createConcurrentIntObjectMap();

    public void add(T element) {
        INDEX.put(element.index(), element);
    }

    public T get(short index) {
        return INDEX.get(index);
    }

    public int size() {
        return INDEX.size();
    }
}
