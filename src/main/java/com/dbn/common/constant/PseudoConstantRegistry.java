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

package com.dbn.common.constant;

import com.dbn.common.util.Commons;
import com.dbn.common.util.Strings;
import com.dbn.common.util.Unsafe;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class PseudoConstantRegistry {
    private static final Map<Class<? extends PseudoConstant>, PseudoConstantData> REGISTRY = new ConcurrentHashMap<>();

    private PseudoConstantRegistry() {}

    static <T extends PseudoConstant<T>> PseudoConstantData<T> get(Class<T> clazz) {
		return Unsafe.cast(Commons.nvl(PseudoConstantData.LOCAL.get(), () -> REGISTRY.computeIfAbsent(clazz, c -> new PseudoConstantData<>(clazz))));
    }

    static <T extends PseudoConstant<T>> T get(Class<T> clazz, String id) {
        if (Strings.isEmpty(id)) return null;
        PseudoConstantData<T> data = get(clazz);
        return data.get(id);
    }

    public static <T extends PseudoConstant<T>> int register(T constant) {
        Class<T> clazz = Unsafe.cast(constant.getClass());
        PseudoConstantData<T> data = get(clazz);
        return data.register(constant);
    }

    static <T extends PseudoConstant<T>> T[] values(Class<T> clazz) {
        PseudoConstantData<T> data = get(clazz);
        return toArray(data.values(), clazz);
    }

    static <T extends PseudoConstant<T>> T[] list(Class<T> clazz, String csvIds) {
        if (Strings.isEmpty(csvIds)) return toArray(Collections.emptyList(), clazz);

        List<T> constants = new ArrayList<>();
        String[] ids = csvIds.split(",");

        for (String id : ids) {
            if (Strings.isNotEmpty(id)) {
                T constant = get(clazz, id.trim());
                constants.add(constant);
            }
        }
        return toArray(constants, clazz);
    }

    private static <T extends PseudoConstant<T>> T[] toArray(Collection<T> constants, Class<T> clazz) {
        return constants.toArray((T[]) Array.newInstance(clazz, constants.size()));
    }
}
