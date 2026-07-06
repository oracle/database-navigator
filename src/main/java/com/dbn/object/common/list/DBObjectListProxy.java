/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.object.common.list;

import com.dbn.common.content.dependency.ContentDependencyAdapter;
import com.dbn.common.content.dependency.VoidContentDependencyAdapter;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.object.common.DBObject;
import lombok.experimental.Delegate;

import java.util.List;
import java.util.function.Supplier;

public class DBObjectListProxy<T extends DBObject> extends StatefulDisposableBase implements DBObjectList<T> {
    private final Supplier<DBObjectList<T>> delegate;

    private DBObjectListProxy(Supplier<DBObjectList<T>> delegate) {
        this.delegate = delegate;
    }

    public static <T extends DBObject> DBObjectList<T> create(Supplier<DBObjectList<T>> delegate) {
        return new DBObjectListProxy<>(delegate);
    }

    @Delegate(excludes = LifecycleMethods.class)
    public DBObjectList<T> getDelegate() {
        return delegate.get();
    }

    @Override
    public void load() {}

    @Override
    public void reload() {}

    @Override
    public void refresh() {}

    @Override
    public void reloadInBackground() {}

    @Override
    public void loadInBackground() {}

    @Override
    public void markDirty() {}

    @Override
    public void setElements(List<T> elements) {}

    @Override
    public ContentDependencyAdapter getDependencyAdapter() {
        return VoidContentDependencyAdapter.INSTANCE;
    }

    @Override
    public void disposeInner() {}

    private interface LifecycleMethods {
        void dispose();
        boolean isDisposed();
        void setDisposed(boolean disposed);
        void disposeInner();
        void load();
        void reload();
        void refresh();
        void reloadInBackground();
        void loadInBackground();
        void markDirty();
        void setElements(List<?> elements);
        ContentDependencyAdapter getDependencyAdapter();
    }
}
