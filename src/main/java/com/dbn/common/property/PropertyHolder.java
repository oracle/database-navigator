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

package com.dbn.common.property;

import com.dbn.nls.NlsSupport;

public interface PropertyHolder<T extends Property> extends NlsSupport {
    boolean set(T property, boolean value);

    boolean is(T property);

    default boolean isNot(T property) {
        return !is(property);
    };

    @SuppressWarnings("unchecked")
    default boolean isOneOf(T... properties) {
        for (T property : properties) {
            if (is(property)) return true;
        }
        return false;
    }

    default void conditional(T property, Runnable runnable) {
        if(isNot(property)) {
            synchronized (this) {
                if (isNot(property)) {
                    try {
                        set(property, true);
                        runnable.run();
                    } finally {
                        set(property, false);
                    }
                }
            }
        }
    }
}
