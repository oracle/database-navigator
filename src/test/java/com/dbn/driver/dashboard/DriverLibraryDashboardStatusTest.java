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
import org.junit.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DriverLibraryDashboardStatusTest {
    @Test
    public void inventoryRefreshIncludesConcreteTypesForAllAndGeneric() {
        List<DatabaseType> expected = List.of(DatabaseType.ORACLE, DatabaseType.MYSQL, DatabaseType.POSTGRES, DatabaseType.SQLITE, DatabaseType.GENERIC);
        assertEquals(expected, DriverLibraryDashboardStatus.typesToRefresh(null));
        assertEquals(expected, DriverLibraryDashboardStatus.typesToRefresh(DatabaseType.GENERIC));
        assertEquals(List.of(DatabaseType.POSTGRES), DriverLibraryDashboardStatus.typesToRefresh(DatabaseType.POSTGRES));
    }

    @Test
    public void genericPackageFilterRemainsWildcardAcrossTypes() {
        var oraclePackage = new com.dbn.driver.download.metadata.DriverPackage("oracle", "Oracle", DatabaseType.ORACLE, List.of());
        var genericPackage = new com.dbn.driver.download.metadata.DriverPackage("generic", "Generic", DatabaseType.GENERIC, List.of());
        oraclePackage.setCloudConfigProviderFamily(CloudConfigProviderFamily.AWS);
        assertTrue(oraclePackage.matches(DatabaseType.GENERIC));
        assertFalse(genericPackage.matches(DatabaseType.ORACLE));
        assertFalse(oraclePackage.matches(DatabaseType.MYSQL));
        assertTrue(oraclePackage.matches(CloudConfigProviderFamily.AWS));
        assertFalse(genericPackage.matches(CloudConfigProviderFamily.AWS));
    }

    @Test
    public void rowStatusPrioritizesActiveAndCompletedStates() {
        assertEquals(DriverLibraryDashboardStatus.RESOLVING,
                DriverLibraryDashboardStatus.resolve(false, true, false, false, false));
        assertEquals(DriverLibraryDashboardStatus.DOWNLOADING,
                DriverLibraryDashboardStatus.resolve(false, false, true, false, false));
        assertEquals(DriverLibraryDashboardStatus.DOWNLOADED,
                DriverLibraryDashboardStatus.resolve(false, false, false, true, false));
        assertEquals(DriverLibraryDashboardStatus.OBSOLETE,
                DriverLibraryDashboardStatus.resolve(false, false, false, true, true));
        assertEquals(DriverLibraryDashboardStatus.FAILED,
                DriverLibraryDashboardStatus.resolve(true, false, false, false, false));
        assertEquals(DriverLibraryDashboardStatus.AVAILABLE,
                DriverLibraryDashboardStatus.resolve(false, false, false, false, false));
    }

    @Test
    public void settingsContextFixesTypeAndSelectionCallsBackWithPath() {
        AtomicReference<String> selectedPath = new AtomicReference<>();
        DriverLibraryDashboardOptions options = new DriverLibraryDashboardOptions(
                DatabaseType.ORACLE,
                CloudConfigProviderFamily.OCI,
                selectedPath::set);

        assertTrue(options.isDatabaseTypeFixed());
        assertTrue(options.canSelectLibrary());
        options.selectLibrary("/drivers/oracle");
        assertEquals("/drivers/oracle", selectedPath.get());

        DriverLibraryDashboardOptions menuOptions = new DriverLibraryDashboardOptions(null, null, null);
        assertFalse(menuOptions.isDatabaseTypeFixed());
        assertFalse(menuOptions.canSelectLibrary());
    }
}
