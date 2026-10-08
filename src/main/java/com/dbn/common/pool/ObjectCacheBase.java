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
import com.dbn.common.exception.Exceptions;
import com.dbn.common.lookup.Visitor;
import com.dbn.common.util.Unsafe;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.progress.ProcessCanceledException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

/**
 * Keyed resource cache. Entry changes are synchronized on the cache; creation,
 * validation, and lifecycle hooks run outside that monitor.
 */
public abstract class ObjectCacheBase<K, O, E extends Throwable> extends StatefulDisposableBase implements ObjectCache<K, O, E> {
    // Guarded by this monitor. Pending entries reserve their key until publication or removal.
    private final Map<K, Entry<O>> data = new HashMap<>();

    public ObjectCacheBase(@Nullable Disposable parent) {
        super(parent);
    }

    @Override
    public synchronized O get(K key) {
        if (isDisposed()) return null;
        Entry<O> entry = data.get(key);
        return entry == null ? null : entry.object;
    }

    @Override
    public synchronized int size() {
        return (int) data.values().stream().filter(entry -> entry.object != null).count();
    }

    @NotNull
    @Override
    public O ensure(K key) throws E {
        try {
            checkDisposed();
            if (key == null) return whenNull();

            while (true) {
                Entry<O> entry;
                boolean create;
                synchronized (this) {
                    checkDisposed();
                    if (Thread.interrupted()) throw new InterruptedException();

                    entry = data.get(key);
                    create = entry == null;
                    if (create) {
                        entry = new Entry<>();
                        data.put(key, entry);
                    } else if (entry.object == null) {
                        if (entry.creator == Thread.currentThread()) {
                            throw new IllegalStateException("Recursive cache initialization");
                        }
                        wait();
                        continue;
                    }
                }

                if (create) {
                    O object = create(key, entry);
                    return object == null ? whenNull() : object;
                }

                O object = entry.object;
                if (!check(object)) {
                    discard(key, entry);
                    continue;
                }

                O result = whenReused(object);
                synchronized (this) {
                    checkDisposed();
                    if (data.get(key) == entry) return result;
                }
            }
        } catch (Throwable e) {
            conditionallyLog(e);
            if (e instanceof ProcessCanceledException cancelled) throw cancelled;
            if (e instanceof Error error) throw error;
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return whenErrored(e);
        }
    }

    private O create(K key, Entry<O> entry) throws E {
        boolean created = false;
        try {
            O object = create(key);
            synchronized (this) {
                entry.object = object;
                entry.creator = null;
                checkDisposed();
                if (object == null || data.get(key) != entry) return null;
                notifyAll();
            }

            // Creation notifications may look up the newly published entry.
            O result = whenCreated(object);
            synchronized (this) {
                checkDisposed();
                if (result == null || data.get(key) != entry) return null;
                created = true;
            }
            return result;
        } finally {
            if (!created) discard(key, entry);
        }
    }

    @Override
    public void discard(O object) {
        if (object == null) return;
        K key = getKey(object);
        Entry<O> entry;
        synchronized (this) {
            entry = data.get(key);
            if (entry == null || entry.object != object) return;
        }
        discard(key, entry);
    }

    private void discard(K key, Entry<O> entry) {
        O object;
        synchronized (this) {
            if (data.get(key) == entry) {
                data.remove(key);
                notifyAll();
            }
            if (entry.object == null || entry.discarded) return;

            entry.discarded = true;
            object = entry.object;
        }
        whenDiscarded(object);
    }

    protected O whenCreated(O object) { return object; }
    protected O whenReused(O object) { return object; }

    /** Releases an object's resources after removal, including during cache disposal. */
    protected O whenDiscarded(O object) {
        Disposer.dispose(object);
        return object;
    }

    @NotNull
    protected O whenErrored(Throwable e) throws E {
        throw Exceptions.toRuntimeException(e);
    }

    @NotNull
    protected O whenNull() throws E {
        throw new IllegalStateException("Failed to initialize cached object");
    }

    public void visit(Visitor<O> visitor) {
        for (O object : snapshot()) {
            visitor.visit(object);
        }
    }

    public void visit(Predicate<O> when, Visitor<O> visitor) {
        for (O object : snapshot()) {
            if (when.test(object)) visitor.visit(object);
        }
    }

    private synchronized List<O> snapshot() {
        return data.values().stream().map(entry -> entry.object).filter(Objects::nonNull).toList();
    }

    @NotNull
    protected abstract O create(K key) throws E;

    /** Returns the stable key used to cache this object. */
    @NotNull
    protected abstract K getKey(O object);

    protected abstract boolean check(@Nullable O object);

    @Override
    public void disposeInner() {
        Map<K, Entry<O>> entries;
        synchronized (this) {
            setDisposed(true);
            entries = new HashMap<>(data);
            data.clear();
        }
        entries.forEach((key, entry) -> Unsafe.warned(() -> discard(key, entry)));
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

    private static final class Entry<O> {
        private Thread creator = Thread.currentThread();
        private O object;
        private boolean discarded;
    }
}
