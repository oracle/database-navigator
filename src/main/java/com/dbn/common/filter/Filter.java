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

package com.dbn.common.filter;

import com.dbn.common.sign.Signed;
import lombok.SneakyThrows;

import java.util.Collection;

public interface Filter<T> extends Signed {
    boolean accepts(T object);

    @Override
    @SneakyThrows
    default int getSignature() {
        return hashCode();
    }

    default boolean acceptsAll(Collection<T> objects) {
        for (T object : objects) {
            if (!accepts(object)) return false;
        }
        return true;
    }

    default boolean isEmpty() {
        return false;
    }
}
