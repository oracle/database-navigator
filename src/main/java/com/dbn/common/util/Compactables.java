package com.dbn.common.util;

import com.dbn.common.list.FilteredList;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.coverage.gnu.trove.THash;

import java.util.*;

@UtilityClass
public class Compactables {
    public static <T extends Compactable> void compact(@Nullable T compactable) {
        if (compactable != null) {
            compactable.compact();
        }
    }

    public static <T extends Collection<E>, E> T compact(@Nullable T elements) {
        if (elements != null) {
            int size = elements.size();
            boolean empty = size == 0;
            boolean single = size == 1;

            if (elements instanceof FilteredList) {
                FilteredList<?> filteredList = (FilteredList<?>) elements;
                filteredList.trimToSize();

            } else  if (elements instanceof List) {
                if (empty) {
                    return Unsafe.cast(Collections.emptyList());
                } else if (single) {
                    return Unsafe.cast(Collections.singletonList(elements.stream().findFirst().orElse(null)));
                } else if (elements instanceof ArrayList){
                    ArrayList<?> arrayList = (ArrayList<?>) elements;
                    arrayList.trimToSize();
                    return Unsafe.cast(arrayList);
                }
            }  else if (elements instanceof Set) {
                if (empty) {
                    return Unsafe.cast(Collections.emptySet());
                } else if (single) {
                    return Unsafe.cast(Collections.singleton(elements.stream().findFirst().orElse(null)));
                } else if (elements instanceof THash){
                    THash hash = (THash) elements;
                    hash.compact();
                    return Unsafe.cast(hash);
                }
            }
        }
        return elements;
    }

    public static <T extends Map<K, V>, K, V> T compact(@Nullable T elements) {
        if (elements != null) {
            int size = elements.size();
            boolean empty = size == 0;
            boolean single = size == 1;

            if (empty) {
                return Unsafe.cast(Collections.emptyMap());
            } else if (single) {
                K key = elements.keySet().stream().findFirst().orElse(null);
                V value = elements.get(key);
                return Unsafe.cast(Collections.singletonMap(key, value));
            } else if (elements instanceof THash) {
                THash hash = (THash) elements;
                hash.compact();
            }
        }
        return elements;
    }
}
