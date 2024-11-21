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

package com.dbn.common.latent;


import com.dbn.common.color.Colors;
import com.dbn.common.latent.impl.BasicLatent;
import com.dbn.common.latent.impl.MutableLatent;
import com.dbn.common.latent.impl.ReloadableLatent;
import com.dbn.common.latent.impl.ThreadLocalLatent;
import com.dbn.common.latent.impl.WeakRefLatent;
import com.dbn.common.routine.ParametricCallable;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static com.dbn.common.dispose.Failsafe.nd;

public interface Latent<T> extends Supplier<T> {
    T value();
    void set(T value);
    void reset();
    boolean loaded();

    @NotNull
    default T ensure() {
        return nd(get());
    }

    static <T> Latent<T> basic(Loader<T> loader) {
        return new BasicLatent<>(loader);
    }

    static <T, M> Latent<T> mutable(Loader<M> mutableLoader, Loader<T> loader) {
        return new MutableLatent<>(mutableLoader, loader);
    }

    static <P, T> Latent<T> reloadable(long interval, TimeUnit intervalUnit, P param, ParametricCallable<P, T, RuntimeException> callable) {
        return new ReloadableLatent<>(interval, intervalUnit, () -> callable.call(param));
    }

    static <T> WeakRefLatent<T> weak(Loader<T> loader) {
        return new WeakRefLatent<>(loader);
    }

    static <T> Latent<T> laf(Loader<T> loader) {
        Latent<T> latent = basic(loader);
        Colors.subscribe(null, () -> latent.reset());
        return latent;
    }


    static <T> ThreadLocalLatent<T> thread(Loader<T> loader) {
        return new ThreadLocalLatent<>(loader);
    }

}
