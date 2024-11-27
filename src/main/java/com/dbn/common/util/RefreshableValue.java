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

package com.dbn.common.util;

public abstract class RefreshableValue<T>{
    private T value;
    private boolean loaded = false;
    private final int refreshInterval;
    private long lastRefreshTimestamp;

    public RefreshableValue(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }

    public T get() {
        if (!loaded || lastRefreshTimestamp < System.currentTimeMillis() - refreshInterval) {
            value = Commons.nvln(load(), value);
            loaded = true;
            lastRefreshTimestamp = System.currentTimeMillis();
        }
        return value;
    }

    protected abstract T load();
}
