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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

enum DriverLibraryDashboardStatus {
    AVAILABLE,
    RESOLVING,
    DOWNLOADING,
    DOWNLOADED,
    OBSOLETE,
    FAILED;

    static List<DatabaseType> typesToRefresh(@Nullable DatabaseType selectedType) {
        if (selectedType != null && selectedType != DatabaseType.GENERIC) {
            return List.of(selectedType);
        }

        List<DatabaseType> databaseTypes = new ArrayList<>(Arrays.asList(DatabaseType.NATIVELY_SUPPORTED));
        databaseTypes.add(DatabaseType.GENERIC);
        return databaseTypes;
    }

    static DriverLibraryDashboardStatus resolve(
            boolean failed,
            boolean resolving,
            boolean downloading,
            boolean downloaded,
            boolean obsolete) {
        if (failed) return FAILED;
        if (resolving) return RESOLVING;
        if (downloading) return DOWNLOADING;
        if (downloaded && obsolete) return OBSOLETE;
        if (downloaded) return DOWNLOADED;
        return AVAILABLE;
    }
}
