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

package com.dbn.common.latent.impl;

import com.dbn.common.latent.Latent;
import com.dbn.common.latent.Loader;
import com.dbn.common.ref.WeakRef;

public class WeakRefLatent<T> implements Latent<T> {
    private final Loader<T> loader;
    private WeakRef<T> value;
    private volatile boolean loaded;

    public WeakRefLatent(Loader<T> loader) {
        this.loader = loader;
    }

    public final T get() {
        if (!loaded) {
            synchronized (this) {
                if (!loaded) {
                    T value = loader == null ? null : loader.load();
                    this.value = WeakRef.of(value);
                    loaded = true;
                }
            }
        }
        return WeakRef.get(value);
    }

    public final void set(T value) {
        this.value = WeakRef.of(value);
        loaded = true;
    }

    public final boolean loaded() {
        return loaded;
    }

    public void reset() {
        value = null;
        loaded = false;
    }

    @Override
    public T value() {
        return WeakRef.get(value);
    }
}
