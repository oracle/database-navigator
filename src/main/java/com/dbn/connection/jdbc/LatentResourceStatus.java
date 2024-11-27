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

package com.dbn.connection.jdbc;

import com.dbn.common.property.Property;
import com.dbn.common.property.PropertyHolder;
import com.dbn.common.ref.WeakRef;
import com.dbn.common.thread.Background;
import com.dbn.common.thread.ThreadMonitor;
import com.dbn.common.util.TimeUtil;
import org.jetbrains.annotations.NotNull;

public abstract class LatentResourceStatus<T extends Property.IntBase> {
    private final long interval;
    private volatile boolean checking;
    private long lastCheck;
    private boolean dirty;
    private final T status;
    private final WeakRef<PropertyHolder<T>> resource;

    protected LatentResourceStatus(PropertyHolder<T> resource, T status, boolean initialValue, long interval){
        resource.set(status, initialValue);
        this.status = status;
        this.interval = interval;
        this.resource = WeakRef.of(resource);
    }

    public boolean check() {
        if (!checking) {
            synchronized (this) {
                if (!checking) {
                    checking = true;
                    long currentTimeMillis = System.currentTimeMillis();
                    if (TimeUtil.isOlderThan(lastCheck, interval) || dirty) {
                        lastCheck = currentTimeMillis;
                        checkControlled();
                    } else {
                        checking = false;
                    }
                }
            }
        }
        return get();
    }

    private void checkControlled() {
        if (ThreadMonitor.isTimeSensitiveThread()) {
            Background.run(null, () -> checkControlled());
        } else {
            boolean oldValue = get();
            try {
                set(doCheck());
            } finally {
                dirty = false;
                checking = false;
                if (get() != oldValue) statusChanged(status);
            }
        }
    }

    public abstract void statusChanged(T status);

    public void set(boolean value) {
        getResource().set(status, value);
    }

    @NotNull
    PropertyHolder<T> getResource() {
        return this.resource.ensure();
    }

    public void markDirty() {
        dirty = true;
    }

    public boolean get() {
        return getResource().is(status);
    }

    protected abstract boolean doCheck();
}
