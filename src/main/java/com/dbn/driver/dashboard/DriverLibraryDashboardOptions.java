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

package com.dbn.driver.dashboard;

import com.dbn.connection.DatabaseType;
import com.dbn.connection.config.provider.CloudConfigProviderFamily;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** Launch context for the driver library dashboard. */
public record DriverLibraryDashboardOptions(
        @Nullable DatabaseType databaseType,
        @Nullable CloudConfigProviderFamily providerFamily,
        @Nullable Consumer<String> selectionCallback) {

    public boolean isDatabaseTypeFixed() {
        return databaseType != null;
    }

    public boolean canSelectLibrary() {
        return selectionCallback != null;
    }

    public void selectLibrary(String path) {
        if (selectionCallback != null) {
            selectionCallback.accept(path);
        }
    }
}
