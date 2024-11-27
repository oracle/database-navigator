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


import com.dbn.common.dispose.Failsafe;
import com.dbn.common.util.Commons;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

import static com.dbn.common.dispose.Failsafe.nd;

public class WeakRef<T> extends WeakReference<T> {
    protected WeakRef(T referent) {
        super(referent);
    }

    @Contract("null -> null;!null -> !null;")
    public static <T> WeakRef<T> of(@Nullable T element) {
        return element == null ? null : new WeakRef<>(element);
    }

    @Nullable
    public static <T> T get(@Nullable WeakRef<T> ref) {
        return ref == null ? null : ref.get();
    }

    @NotNull
    public static <T> T ensure(@Nullable WeakRef<T> ref) {
        T object = get(ref);
        return nd(object);
    }

    @Nullable
    @Override
    public T get() {
        return super.get();
    }

    @NotNull
    public T ensure() {
        return Failsafe.nn(get());
    }

    @Override
    public void clear() {
        super.clear();
    }

    @Override
    public int hashCode() {
        T referent = get();
        return referent == null ? -1 : referent.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WeakRef) {
            WeakRef<?> that = (WeakRef<?>) obj;
            return Commons.match(this, that, ref -> ref.get());
        }
        return false;
    }
}
