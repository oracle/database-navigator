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

package com.dbn.oci.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

@UtilityClass
public class OciIdentifiers {
    public static boolean isUserOcid(String value) {
        return isOcid(value, "user");
    }

    public static boolean isTenancyOcid(String value) {
        return isOcid(value, "tenancy");
    }

    public static boolean isCompartmentOcid(String value) {
        return isOcid(value, "compartment");
    }

    public static boolean isCompartmentScopeOcid(String value) {
        return isCompartmentOcid(value) || isTenancyOcid(value);
    }

    private static boolean isOcid(@NonNls String value, @NonNls String resourceType) {
        if (value == null) return false;
        value = value.trim();

        String prefix = "ocid1." + resourceType + ".";
        if (!value.startsWith(prefix)) return false;

        String suffix = value.substring(prefix.length());
        int realmSeparator = suffix.indexOf('.');
        return realmSeparator > 0 && realmSeparator < suffix.length() - 1;
    }
}
