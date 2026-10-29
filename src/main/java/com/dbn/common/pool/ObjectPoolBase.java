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
import com.dbn.common.util.Unsafe;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProcessCanceledException;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/**
 * Circular object pool. Ownership changes are synchronized on the pool; resource
 * operations and lifecycle hooks run outside that monitor.
 * @param <O> the type of object this pool is offering
 */
@Slf4j
public abstract class ObjectPoolBase<O, E extends Throwable> extends StatefulDisposableBase implements ObjectPool<O, E> {
    // Guarded by this monitor. Owned objects absent from available and reserved
    // are being acquired, released, or cleaned.
    private final Set<O> objects = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Set<O> reserved = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Deque<O> available = new ArrayDeque<>();
    private final ObjectPoolCounters counters = new ObjectPoolCounters();

    public ObjectPoolBase(@Nullable Disposable parent) {
        super(parent);
    }

    @Override
    public final O acquire(long timeout, TimeUnit timeUnit) throws E {
        counters.waiting().increment();
        try {
            long deadline = System.nanoTime() + Math.max(0, timeUnit.toNanos(timeout));
            do {
                O object = ensure(deadline);
                if (object == null) break;

                O acquired = acquire(object);
                if (acquired != null) return acquired;
            } while (deadline - System.nanoTime() > 0);

            counters.rejected().increment();
            log("rejected", null);
            return whenNull();
        } catch (Throwable e) {
            conditionallyLog(e);
            if (e instanceof ProcessCanceledException cancelled) throw cancelled;
            if (e instanceof Error error) throw error;
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return whenErrored(e);
        } finally {
            counters.waiting().decrement();
        }
    }

    private O acquire(O object) {
        boolean acquired = false;
        try {
            if (!check(object)) return null;

            O result = Objects.requireNonNull(whenAcquired(object));
            log("acquired", object);
            synchronized (this) {
                checkDisposed();
                if (!objects.contains(object)) return null;

                reserved.add(object);
                counters.reserved().increment();
                acquired = true;
            }
            return result;
        } finally {
            if (!acquired) discard(object);
        }
    }

    @Override
    public final O release(O object) {
        synchronized (this) {
            if (!reserved.remove(object)) return object;
            counters.reserved().decrement();
        }

        boolean released = false;
        try {
            if (isDisposed() || !check(object)) return object;

            whenReleased(object);
            log("released", object);
            released = reuse(object);
        } catch (Throwable e) {
            conditionallyLog(e);
            if (e instanceof ProcessCanceledException cancelled) throw cancelled;
            if (e instanceof Error error) throw error;
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
        } finally {
            if (!released) discard(object);
        }
        return object;
    }

    private synchronized boolean reuse(O object) {
        if (isDisposed() || !objects.contains(object)) return false;

        available.addLast(object);
        notifyAll();
        return true;
    }

    @Override
    public final O discard(O object) {
        synchronized (this) {
            if (!objects.remove(object)) return object;

            available.removeIf(candidate -> candidate == object);
            if (reserved.remove(object)) counters.reserved().decrement();
            notifyAll();
        }
        O result = whenDiscarded(object);
        log("discarded", object);
        return result;
    }

    /** Removes matching idle objects without interfering with borrowers. */
    public final void clean(Predicate<O> when) {
        List<O> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(available);
        }
        for (O object : snapshot) {
            synchronized (this) {
                if (!available.removeIf(candidate -> candidate == object)) continue;
            }
            boolean remove = false;
            try {
                remove = when.test(object);
            } finally {
                if (remove || !reuse(object)) discard(object);
            }
        }
    }

    private O ensure(long deadline) throws E, InterruptedException {
        synchronized (this) {
            while (true) {
                checkDisposed();
                if (Thread.interrupted()) throw new InterruptedException();

                O object = available.pollFirst();
                if (object != null) return object;
                if (size() < maxSize()) {
                    counters.creating().increment();
                    break;
                }

                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) return null;
                TimeUnit.NANOSECONDS.timedWait(this, remaining);
                checkDisposed();
                if (deadline - System.nanoTime() <= 0) return null;
            }
        }

        O object = null;
        boolean created = false;
        try {
            object = Objects.requireNonNull(create());
            whenCreated(object);
            log("created", object);
            created = true;
        } finally {
            synchronized (this) {
                counters.creating().decrement();
                if (created && !isDisposed()) {
                    objects.add(object);
                    counters.peak().max(objects.size());
                } else {
                    created = false;
                }
                notifyAll();
            }
            if (!created && object != null) {
                whenDiscarded(object);
                log("discarded", object);
            }
        }
        checkDisposed();
        return object;
    }

    protected O whenCreated(O object) { return object; }
    protected O whenAcquired(O object) { return object; }
    protected O whenReleased(O object) throws E { return object; }

    /** Releases an object's resources after removal, including during pool disposal. */
    protected O whenDiscarded(O object) {
        Disposer.dispose(object);
        return object;
    }

    protected O whenErrored(Throwable e) throws E { return null; }
    protected O whenNull() throws E { return null; }

    @NonNls
    protected String identifier() { return "Object Pool"; }

    @NonNls
    protected String identifier(O object) { return object == null ? "Object" : object.toString(); }

    public abstract int maxSize();

    protected abstract O create() throws E;

    protected abstract boolean check(O object);

    public final synchronized int size() {
        return counters.creating().get() + objects.size();
    }

    @Override
    public int peakSize() {
        return counters.peak().get();
    }

    public final void visit(Visitor<O> visitor) {
        List<O> snapshot;
        synchronized (this) {
            snapshot = new ArrayList<>(objects);
        }
        for (O object : snapshot) {
            visitor.visit(object);
        }
    }

    @Override
    public void disposeInner() {
        setDisposed(true);
        visit(object -> Unsafe.warned(() -> discard(object)));
    }

    @Override
    public synchronized boolean isDisposed() {
        return super.isDisposed();
    }

    @Override
    public synchronized void setDisposed(boolean disposed) {
        super.setDisposed(disposed);
        notifyAll();
    }

    private void log(@NonNls String action, O object) {
        int size;
        int free;
        synchronized (this) {
            size = objects.size();
            free = available.size();
        }
        log.info("{}: {} {} - Pool [max={} size={} peak={} waiting={} free={}]",
                identifier(),
                action,
                identifier(object),
                maxSize(),
                size,
                counters.peak().get(),
                counters.waiting().get(),
                free);
    }


}
