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

package com.dbn.common.action;

import com.dbn.common.Reflection;
import com.dbn.common.compatibility.Workaround;
import com.dbn.common.ref.WeakRef;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataProvider;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.action.DataKeys.PARENT_DISPOSABLE;

@Deprecated // TODO consider using com.intellij.openapi.actionSystem.CompositeDataProvider
public class CompositeDataProvider implements DataProvider {
    private final WeakRef<DataProviderDelegate> dataProviderDelegate;
    private final WeakRef<DataProvider> dataProvider;

    public CompositeDataProvider(DataProviderDelegate dataProviderDelegate, DataProvider dataProvider) {
        this.dataProviderDelegate = WeakRef.of(dataProviderDelegate);
        this.dataProvider = WeakRef.of(dataProvider);
    }

    @Nullable
    public DataProviderDelegate getDataProviderDelegate() {
        return WeakRef.get(dataProviderDelegate);
    }

    @Nullable
    public DataProvider getDataProvider() {
        return WeakRef.get(dataProvider);
    }

    @Override
    public @Nullable Object getData(@NotNull @NonNls String dataId) {
        DataProvider provider = getDataProvider();
        Object data = getData(provider, dataId);
        if (data != null) return data;

        DataProviderDelegate delegate = getDataProviderDelegate();
        if (delegate == null) return null;

        if (delegate instanceof Disposable && PARENT_DISPOSABLE.is(dataId)) {
            return delegate;
        }

        return delegate.getData(dataId);
    }

    @Workaround // OverrideOnly DataProvider
    private static @Nullable Object getData(DataProvider provider, String dataId) {
        if (provider == null) return null;
        return Reflection.invokeMethod(provider, "getData", dataId);
    }
}
