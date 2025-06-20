/*
 * Copyright 2025 Oracle and/or its affiliates
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
import com.dbn.common.routine.ParametricCallable;
import lombok.experimental.UtilityClass;

import java.util.concurrent.TimeUnit;

@UtilityClass
public class LatentFactory {

    public static <T> Latent<T> basic(Loader<T> loader) {
        return new BasicLatent<>(loader);
    }

    public static <T, S> Latent<T> mutable(Loader<S> signatureLoader, Loader<T> valueLoader) {
        return new MutableLatent<>(signatureLoader, valueLoader);
    }

    public static <T> Latent<T> recursive(Loader<T> loader) {
        return new RecursiveLatent<>(loader);
    }

    public static <P, T> Latent<T> reloadable(long interval, TimeUnit intervalUnit, P param, ParametricCallable<P, T, RuntimeException> callable) {
        return new ReloadableLatent<>(interval, intervalUnit, () -> callable.call(param));
    }

    public static <T> Latent<T> weak(Loader<T> loader) {
        return new WeakRefLatent<>(loader);
    }

    public static <T> Latent<T> thread(Loader<T> loader) {
        return new ThreadLocalLatent<>(loader);
    }
}
