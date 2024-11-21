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
import com.dbn.common.util.TimeUtil;

import java.util.concurrent.TimeUnit;

public class ReloadableLatent<T, M> extends BasicLatent<T> implements Latent<T> {
    private long timestamp;
    private final long intervalMillis;

    public ReloadableLatent(long interval, TimeUnit intervalUnit, Loader<T> loader) {
        super(loader);
        intervalMillis = intervalUnit.toMillis(interval);
    }

    @Override
    protected boolean shouldLoad(){
        return super.shouldLoad() || TimeUtil.isOlderThan(timestamp, intervalMillis);
    }

    @Override
    protected void beforeLoad() {
        timestamp = System.currentTimeMillis();
    }
}
