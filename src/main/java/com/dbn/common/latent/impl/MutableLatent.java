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
import com.dbn.common.util.Safe;

import java.util.Objects;

public class MutableLatent<T, M> extends BasicLatent<T> implements Latent<T> {
    private M mutable;
    private final Loader<M> mutableLoader;

    public MutableLatent(Loader<M> mutableLoader, Loader<T> loader) {
        super(loader);
        this.mutableLoader = mutableLoader;
    }

    @Override
    protected boolean shouldLoad(){
        if (super.shouldLoad()) return true;

        return mutable != null && !Objects.equals(mutable, loadMutable());
    }

    @Override
    protected void beforeLoad() {
        mutable = loadMutable();
    }

    private M loadMutable() {
        return Safe.call(mutableLoader, ml -> ml.load());
    }
}
