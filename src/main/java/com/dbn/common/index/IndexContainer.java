package com.dbn.common.index;

import com.dbn.common.util.Compactable;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
public class IndexContainer<T extends Indexable> implements Compactable {
    private final IndexCollection INDEX = new IndexCollection();

    public void add(T element) {
        INDEX.add(element.index());
    }

    public boolean isEmpty() {
        return INDEX.isEmpty();
    }

    public boolean contains(T indexable) {
        return INDEX.contains(indexable.index());
    }

    public Set<T> elements(IndexResolver<T> resolver) {
        if (INDEX.isEmpty()) {
            return Collections.emptySet();
        } else {
            Set<T> elements = new HashSet<>(INDEX.size());
            int[] values = INDEX.values();
            for (int value : values) {
                T element = resolver.apply(value);
                if (element != null) {
                    elements.add(element);
                }
            }
            return elements;
        }
    }

    @Override
    public void compact() {
        //INDEX.trimToSize();
    }

    public void addAll(Collection<T> elements) {
        for (T element : elements) {
            INDEX.add(element.index());
        }
    }


    @FunctionalInterface
    public interface IndexResolver<R> {
        R apply(int index);
    }
}
