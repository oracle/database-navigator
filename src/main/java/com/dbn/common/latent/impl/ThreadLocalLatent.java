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

public final class ThreadLocalLatent<T> implements Latent<T> {
    private final Loader<T> loader;
    private final ThreadLocal<T> value = new ThreadLocal<>();

    public ThreadLocalLatent(Loader<T> loader) {
        this.loader = loader;
    }

    @Override
    public void set(T value) {
        this.value.set(value);
    }

    @Override
    public void reset() {
        this.value.remove();
    }

    @Override
    public boolean loaded() {
        return this.value.get() != null;
    }

    public T get(){
        T value = this.value.get();
        if (value == null) {
            value = loader.load();
            this.value.set(value);
        }
        return value;
    }

    @Override
    public T value() {
        return value.get();
    }
}
