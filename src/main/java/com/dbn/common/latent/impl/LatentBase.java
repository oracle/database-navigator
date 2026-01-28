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
    private transient final Loader<T> loader;
    private transient T value;
    private transient boolean loaded;

    public LatentBase(Loader<T> loader) {
        this.loader = loader;
    }

    public final synchronized T get(){
        if (shouldLoad()) {
            try {
                beforeLoad();
                load();
            } finally {
                afterLoad();
            }
        }
        return value;
    }

    private void load() {
        // NOTE even if final, the loader could become null through
        // forced disposal utilities like com.dbn.common.dispose.Nullifier
        Loader<T> loader = this.loader == null ? () -> value : this.loader;
        value = loader.load();
        loaded = true;
    }

    protected boolean shouldLoad() {
        return !loaded;
    }

    protected void beforeLoad() {};

    protected void afterLoad() {}

    public final synchronized void set(T value) {
        this.value = value;
        this.loaded = true;
    }

    public final synchronized boolean loaded() {
        return loaded;
    }

    @Override
    public final synchronized T value() {
        return value;
    }

    public synchronized void reset() {
        value = null;
        loaded = false;
    }
}
