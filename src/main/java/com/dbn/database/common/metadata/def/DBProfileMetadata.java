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

package com.dbn.database.common.metadata.def;

import com.dbn.database.common.metadata.DBObjectMetadata;
import com.dbn.database.common.security.ObjectIdentifier;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.sql.SQLException;

public interface DBProfileMetadata extends DBObjectMetadata {

    @ObjectIdentifier
    String getProfileName() throws SQLException;

    @ObjectIdentifier
    String getCredentialName() throws SQLException;

    String getDescription() throws SQLException;

    String getProvider() throws SQLException;

    String getModel() throws SQLException;

    double getTemperature() throws SQLException;

    String getObjectList() throws SQLException;

    boolean isEnabled() throws SQLException;

    @Data
    @AllArgsConstructor
    class Record implements DBProfileMetadata {
        private final String profileName;
        private final String credentialName;
        private final String provider;
        private final String model;
        private final String description;
        private final String objectList;
        private final double temperature;
        private final boolean enabled;
    }
}
