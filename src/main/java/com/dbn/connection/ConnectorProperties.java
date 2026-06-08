/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.connection;

import com.dbn.common.util.Maps;
import org.jetbrains.annotations.NonNls;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConnectorProperties {
    private final Map<@NonNls String, @NonNls String> properties = new HashMap<>();

    public void add(@NonNls String key, @NonNls String value) {
        properties.put(key, value);
    }

    public void addAll(Map<String, String> properties) {
        this.properties.putAll(properties);
    }

    public void addMissing(Map<String, String> properties) {
        for (String key : properties.keySet()) {
            String value = properties.get(key);
            this.properties.putIfAbsent(key, value);
        }
    }

    public Properties export() {
        return Maps.toProperties(properties);
    }
}
