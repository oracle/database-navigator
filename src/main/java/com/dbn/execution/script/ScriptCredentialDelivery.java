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

package com.dbn.execution.script;

import com.dbn.common.util.Strings;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public enum ScriptCredentialDelivery {
    TEMP_FILE,
    ENVIRONMENT;

    public static final @NonNls String PROPERTY_NAME = "dbn.script.credentials.delivery";

    public static ScriptCredentialDelivery current() {
        return resolve(System.getProperty(PROPERTY_NAME));
    }

    public static ScriptCredentialDelivery resolve(@Nullable String value) {
        if (Strings.isEmptyOrSpaces(value)) return TEMP_FILE;

        for (ScriptCredentialDelivery delivery : values()) {
            if (delivery.name().equalsIgnoreCase(value.trim())) {
                return delivery;
            }
        }
        return TEMP_FILE;
    }
}
