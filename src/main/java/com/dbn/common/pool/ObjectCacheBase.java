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

import com.dbn.common.collections.ConcurrentOptionalValueMap;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.lookup.Visitor;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public abstract class ObjectCacheBase<K, O, E extends Throwable> extends StatefulDisposableBase implements ObjectCache<K, O, E> {
    private final Map<K, O> data = new ConcurrentOptionalValueMap<>();

    public ObjectCacheBase(@Nullable Disposable parent) {
        super(parent);
    }

    @Override
    public O get(K key) {
        return data.get(key);
    }

    @Override
    public int size() {
        return data.size();
    }

    @NotNull
    @Override
    public O ensure(K key) throws E {
        checkDisposed();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        O object = data.compute(key, (k, o) -> {
            if (check(o)) return whenReused(o);

            if (o != null) replace(o);

            try {
                o = create(k);
                return whenCreated(o);
            } catch (Throwable e) {
                conditionallyLog(e);
                failure.set(e);
                return null;
            }
        });

        Throwable throwable = failure.get();
        if (throwable != null) return whenErrored(throwable);
        if (object == null) return whenNull();
        return object;
    }

    @Override
    public void drop(K key) {
        O object = data.remove(key);
        whenDropped(object);
    }

    private void replace(O object) {
        whenDropped(object);
    }

    protected O whenCreated(O object) { return object; }
    protected O whenReused(O object) { return object; }
    protected O whenDropped(O object) { return object; }
    protected O whenErrored(Throwable e) throws E { return null; }
    protected O whenNull() throws E { return null; }


    public void visit(Visitor<O> visitor) {
        data.values().stream().filter(o -> o != null).forEach(o -> visitor.visit(o));
    }

    public void visit(Predicate<O> when, Visitor<O> visitor) {
        data.values().stream().filter(o -> o != null && when.test(o)).forEach(o -> visitor.visit(o));
    }

    @NotNull
    protected abstract O create(K key) throws E;

    protected abstract boolean check(@Nullable O object);


    @Override
    public void disposeInner() {
        data.clear();
    }
}
