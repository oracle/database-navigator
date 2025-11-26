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

/**
 * A class representing a recursive latent value implementation. The RecursiveLatent class extends
 * {@link LatentBase} and incorporates a mechanism to prevent recursive loading scenarios by
 * utilizing thread-local state.
 *
 * @param <T> the type of value being lazily loaded and managed
 */
final class RecursiveLatent<T> extends LatentBase<T> implements Latent<T> {
    private static final Object LOCK = new Object();
    private final transient ThreadLocal<Object> loading = new ThreadLocal<>();

    RecursiveLatent(Loader<T> loader) {
        super(loader);
    }

    @Override
    protected void beforeLoad() {
        loading.set(LOCK);
    }

    @Override
    protected void afterLoad() {
        loading.remove();
    }

    @Override
    protected boolean shouldLoad() {
        return super.shouldLoad() && !isLoading();
    }

    private boolean isLoading() {
        return loading.get() != null;
    }
}
