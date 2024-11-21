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

public class BasicLatent<T> implements Latent<T> {
    private final Loader<T> loader;
    private T value;
    private volatile boolean loaded;

    public BasicLatent(Loader<T> loader) {
        this.loader = loader;
    }

    public final T get(){
        if (!shouldLoad()) return value;

        synchronized (this) {
            if (!shouldLoad()) return value;

            beforeLoad();
            T newValue = loader == null ? value : loader.load();
            if (value != newValue) {
                value = newValue;
            }
            afterLoad(newValue);
        }
        return value;
    }

    protected boolean shouldLoad() {
        return !loaded;
    }

    protected void beforeLoad() {};

    protected void afterLoad(T value) {
        loaded = true;
    }

    public final void set(T value) {
        this.value = value;
        loaded = true;
    }

    public final boolean loaded() {
        return loaded;
    }

    @Override
    public T value() {
        return value;
    }

    public void reset() {
        value = null;
        loaded = false;
    }
}
