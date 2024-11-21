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

package com.dbn.common.dispose;

import com.intellij.openapi.Disposable;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@UtilityClass
public final class DisposableContainers {

    public static <T extends Disposable> List<T> list(Disposable parent) {
        return new DisposableList<>(parent);
    }

    public static <T extends Disposable> List<T> concurrentList(Disposable parent) {
        return new DisposableConcurrentList<>(parent);
    }

    public static <K, V extends Disposable> Map<K, V> map(Disposable parent) {
        return new DisposableMap<>(parent);
    }

    private static class DisposableList<T extends Disposable> extends ArrayList<T> implements Disposable{
        public DisposableList(@NotNull Disposable parent) {
            Disposer.register(parent, this);
        }

        @Override
        public void dispose() {
            BackgroundDisposer.queue(() -> Disposer.disposeCollection(this));
        }

        @Override
        public T remove(int index) {
            T removed = super.remove(index);
            Disposer.dispose(removed);
            return removed;
        }

        @Override
        public boolean remove(Object o) {
            boolean removed = super.remove(o);
            if (removed && o instanceof Disposable) {
                Disposable disposable = (Disposable) o;
                Disposer.dispose(disposable);
            }
            return removed;
        }
    }

    private static class DisposableConcurrentList<T extends Disposable> extends CopyOnWriteArrayList<T> implements Disposable{
        public DisposableConcurrentList(@NotNull Disposable parent) {
            Disposer.register(parent, this);
        }

        @Override
        public void dispose() {
            BackgroundDisposer.queue(() -> Disposer.disposeCollection(this));
        }

        @Override
        public T remove(int index) {
            T removed = super.remove(index);
            Disposer.dispose(removed);
            return removed;
        }

        @Override
        public boolean remove(Object o) {
            boolean removed = super.remove(o);
            if (removed && o instanceof Disposable) {
                Disposable disposable = (Disposable) o;
                Disposer.dispose(disposable);
            }
            return removed;
        }
    }


    private static class DisposableMap<K, V extends Disposable> extends HashMap<K, V> implements Disposable{
        public DisposableMap(@NotNull Disposable parent) {
            Disposer.register(parent, this);
        }

        @Override
        public void dispose() {
            Disposer.disposeMap(this);
        }

        @Override
        public boolean remove(Object key, Object value) {
            boolean removed = super.remove(key, value);
            if (removed && value instanceof Disposable) {
                Disposable disposable = (Disposable) value;
                Disposer.dispose(disposable);
            }
            return removed;
        }
    }
}
