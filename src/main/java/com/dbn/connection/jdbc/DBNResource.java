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

import com.dbn.common.ui.util.Listeners;
import com.dbn.common.util.Strings;
import com.dbn.common.util.TimeUtil;
import com.dbn.common.util.UUIDs;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.diagnostics.Diagnostics.isDatabaseResourceDebug;

@Getter
@Slf4j
public abstract class DBNResource<T> extends ResourceStatusHolder implements Resource{
    private final long initTimestamp = System.currentTimeMillis();
    private final ResourceType resourceType;
    private final String resourceId = UUIDs.compact();
    protected final T inner;

    private ResourceStatusAdapter<CloseableResource> closed;
    private ResourceStatusAdapter<CancellableResource> cancelled;
    private final Listeners<DBNResourceListener> listeners = Listeners.create();

    private final Map<String, Long> errorLogs = new ConcurrentHashMap<>();

    DBNResource(T inner, ResourceType type) {
        if (inner instanceof DBNResource) {
            throw new IllegalArgumentException("Resource already wrapped");
        }

        this.inner = inner;
        this.resourceType = type;

        if (this instanceof CloseableResource) {
            CloseableResource closeable = (CloseableResource) this;
            closed = new ResourceStatusAdapterImpl<>(closeable,
                    ResourceStatus.CLOSED,
                    ResourceStatus.CHANGING_CLOSED,
                    ResourceStatus.EVALUATING_CLOSED,
                    TimeUtil.Millis.FIVE_SECONDS,
                    Boolean.FALSE,
                    Boolean.TRUE) {
                @Override
                protected void changeInner(boolean value) throws SQLException {
                    closeable.set(ResourceStatus.VALID, false);
                    closeable.closeInner();
                }

                @Override
                protected boolean checkInner() throws SQLException {
                    return closeable.isClosedInner();
                }
            };
        }

        if (this instanceof CancellableResource) {
            CancellableResource cancellable = (CancellableResource) this;
            cancelled = new ResourceStatusAdapterImpl<>(cancellable,
                    ResourceStatus.CANCELLED,
                    ResourceStatus.CHANGING_CANCELLED,
                    ResourceStatus.EVALUATING_CANCELLED,
                    TimeUtil.Millis.FIVE_SECONDS,
                    Boolean.FALSE,
                    Boolean.TRUE) {
                @Override
                protected void changeInner(boolean value) throws SQLException {
                    cancellable.cancelInner();
                }

                @Override
                protected boolean checkInner() throws SQLException {
                    return cancellable.isCancelledInner();
                }


            };
        }

        if (isDatabaseResourceDebug()) log.info("[DBN] Created " + this);
    }

    @Override
    public final String toString() {
        // IMPORTANT:
        // Used for logging of the Resource lifecycle.
        // Changes here have to ensure obscurity of the contents of this resource.
        String string = resourceType + " (" + resourceId + ")";
        String suffix = super.toString();
        return Strings.isEmpty(suffix) ? string : string + " - " + suffix + "";
    }

    @Override
    public void statusChanged(ResourceStatus status) {
    }

    @Override
    public boolean isObsolete() {
        return isClosed();
    }

    public boolean isClosed() {
        return closed.get();
    }

    public void close() throws SQLException {
        closed.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void cancel() throws SQLException {
        cancelled.set(true);
    }

    public boolean shouldNotify(String error) {
        long timestamp = System.currentTimeMillis();
        long lastTimestamp = errorLogs.computeIfAbsent(error, e -> 0L);
        errorLogs.put(error, timestamp);
        return TimeUtil.isOlderThan(lastTimestamp, TimeUtil.Millis.THIRTY_SECONDS);
    }

    public void beforeClose(Runnable runnable) {
        listeners.add(new DBNResourceListener() {
            @Override
            public void closing() {
                runnable.run();
            }
        });
    }

    public void afterClose(Runnable runnable) {
        listeners.add(new DBNResourceListener() {
            @Override
            public void closed() {
                runnable.run();
            }
        });
    }
}
