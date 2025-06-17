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

final class MutableLatent<T, S> extends LatentBase<T> implements Latent<T> {
    private volatile S signature;
    private final Loader<S> signatureLoader;

    MutableLatent(Loader<S> signatureLoader, Loader<T> valueLoader) {
        super(valueLoader);
        this.signatureLoader = signatureLoader;
    }

    @Override
    protected boolean shouldLoad(){
        if (super.shouldLoad()) return true;

        S currentSignature = loadSignature();
        return signature == null ?
                currentSignature != null :
                !Objects.equals(signature, currentSignature);
    }

    @Override
    protected void beforeLoad() {
        signature = loadSignature();
    }

    private S loadSignature() {
        return Safe.call(signatureLoader, ml -> ml.load());
    }
}
