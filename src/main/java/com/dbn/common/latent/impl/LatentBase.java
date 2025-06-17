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

abstract class LatentBase<T> implements Latent<T> {
    private final Loader<T> loader;
    private T value;
    private volatile boolean loaded;

    public LatentBase(Loader<T> loader) {
        this.loader = loader;
    }

    public final T get(){
        if (!shouldLoad()) return value;

        // deferred sync bloc (99% of calls act on a "loaded" latent)
        synchronized (this) {
            if (!shouldLoad()) return value;

            T newValue = null;
            try {
                beforeLoad();
                newValue = loader.load();
                if (value != newValue) {
                    value = newValue;
                }
                loaded = true;
            } finally {
                afterLoad(newValue);
            }
        }
        return value;
    }

    protected boolean shouldLoad() {
        // NOTE even if final, the loader could become null through
        // forced disposal utilities like com.dbn.common.dispose.Nullifier
        return !loaded && loader != null;
    }

    protected void beforeLoad() {};

    protected void afterLoad(T value) {
    }

    public final void set(T value) {
        synchronized (this) {
            this.value = value;
            this.loaded = true;
        }
    }

    public final boolean loaded() {
        return loaded;
    }

    @Override
    public T value() {
        return value;
    }

    public void reset() {
        synchronized (this) {
            value = null;
            loaded = false;
        }
    }
}
