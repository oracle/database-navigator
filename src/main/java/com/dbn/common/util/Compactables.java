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

package com.dbn.common.util;

import com.dbn.common.list.FilteredList;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
            }
        }
        return elements;
    }
}
