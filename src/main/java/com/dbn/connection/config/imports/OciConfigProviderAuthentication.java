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

package com.dbn.connection.config.imports;

import com.dbn.common.ui.Presentable;
import lombok.Getter;

@Getter
public enum OciConfigProviderAuthentication implements Presentable {
    OCI_DEFAULT("OCI Default", "OCI_DEFAULT"),
    OCI_INTERACTIVE("Interactive", "OCI_INTERACTIVE");

    private final String name;
    private final String parameterValue;

    OciConfigProviderAuthentication(String name, String parameterValue) {
        this.name = name;
        this.parameterValue = parameterValue;
    }

    public static OciConfigProviderAuthentication get(String name) {
        if (name == null) return null;

        for (OciConfigProviderAuthentication value : values()) {
            if (value.parameterValue.equalsIgnoreCase(name)) {
                return value;
            }
        }
        return null;
    }
}
