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

package com.dbn.common.ref;

import org.jetbrains.annotations.NotNull;

/**
 * This Wrapper class provides a generic mechanism to wrap and manage a reference to a target object
 * held as a {@link WeakRef}. This ensures that the target can be garbage collected when there are no
 * strong references to it, preventing memory leaks.
 *
 * @param <T> The type of the target object.
 */
public class WeakRefWrapper<T> {
    private final WeakRef<T> target;

    public WeakRefWrapper(@NotNull T target) {
        this.target = WeakRef.of(target);
    }

    @NotNull
    public T getTarget() {
        return WeakRef.ensure(target);
    }
}
