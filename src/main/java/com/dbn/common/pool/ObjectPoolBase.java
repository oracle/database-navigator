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

package com.dbn.common.pool;


import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.lookup.Visitor;
import com.dbn.nls.NlsSupport;
import com.intellij.openapi.Disposable;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/**
 * Circular object pool
 * @param <O> the type of object this pool is offering
 */
@Slf4j
public abstract class ObjectPoolBase<O, E extends Throwable> extends StatefulDisposableBase implements ObjectPool<O, E>, NlsSupport {
    private final List<O> objects = new CopyOnWriteArrayList<>();
    private final BlockingQueue<O> available = new LinkedBlockingQueue<>();
    private final ObjectPoolCounters counters = new ObjectPoolCounters();

    public ObjectPoolBase(@Nullable Disposable parent) {
        super(parent);
    }

    @Override
    public final O acquire(long timeout, TimeUnit timeUnit) throws E {
        try {
            counters.waiting().increment();
            ensure();

            O object = available.poll(timeout, timeUnit);

            if (object == null) {
                counters.rejected().increment();
                log("rejected", null);
                return whenNull();
            }
            if (check(object)) {
                // valid object
                counters.reserved().increment();
                log("acquired", object);
                return whenAcquired(object);
            }

            // invalid object - remove and try acquiring again
            drop(object);
            return acquire(timeout, timeUnit);
        } catch (Throwable e) {
            conditionallyLog(e);
            return whenErrored(e);
        } finally {
            counters.waiting().decrement();
        }
    }

    @Override
    public final O release(O object) {
        checkDisposed();
        if (check(object)) return reuse(object);
        return drop(object);
    }

    private O reuse(O object) {
        try {
            counters.reserved().decrement();
            whenReleased(object);
            available.add(object);
            log("released", object);

        } catch (Throwable e) {
            conditionallyLog(e);
            return drop(object);
        }

        return object;
    }

    @Override
    public final O drop(O object) {
        synchronized (this) {
            available.remove(object);
            objects.remove(object);
        }
        log("dropped", object);
        return whenDropped(object);
    }

    public final void clean(Predicate<O> when) {
        for (O object : objects) {
            if (when.test(object)) {
                drop(object);
            }
        }
    }

    private void ensure() throws E{
        checkDisposed();

        synchronized (this) {
            boolean belowMax = counters.creating().get() + objects.size() < maxSize();
            boolean create = available.peek() == null && belowMax;
            if (create) {
                counters.creating().increment();
            } else {
                return;
            }
        }

        try {
            O object = create();
            objects.add(object);
            available.add(object);
            whenCreated(object);
            log("created", object);
        } finally {
            counters.creating().decrement();
            if (objects.size() > peakSize()) {
                counters.peak().set(objects.size());
            }
        }
    }

    protected O whenCreated(O object) { return object; }
    protected O whenAcquired(O object) { return object; }
    protected O whenReleased(O object) throws E { return object; }
    protected O whenDropped(O object) { return object; }
    protected O whenErrored(Throwable e) throws E { return null; }
    protected O whenNull() throws E { return null; }

    @NonNls
    protected String identifier() { return "Object Pool"; }

    @NonNls
    protected String identifier(O object) { return object == null ? "Object" : object.toString(); }

    public abstract int maxSize();

    protected abstract O create() throws E;

    protected abstract boolean check(O object);

    public final int size() {
        return counters.creating().get() + objects.size();
    }

    @Override
    public int peakSize() {
        return counters.peak().get();
    }

    public final void visit(Visitor<O> visitor) {
        for (O object : objects) {
            visitor.visit(object);
        }
    }

    @Override
    public void disposeInner() {
        available.clear();
        Disposer.disposeCollection(objects);
    }

    private void log(@NonNls String action, O object) {
        log.info("{}: {} {} - Pool [max={} size={} peak={} waiting={} free={}]",
                identifier(),
                action,
                identifier(object),
                maxSize(),
                objects.size(),
                counters.peak().get(),
                counters.waiting().get(),
                available.size());
    }


}
