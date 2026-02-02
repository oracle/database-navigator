/*
 * Copyright 2024-2025 Oracle and/or its affiliates
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

package com.dbn.ml.backend;

import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.backend.dbms.DBMSBackend;
import com.dbn.ml.backend.tribuo.TribuoBackend;
import lombok.experimental.UtilityClass;

/**
 * Factory for creating ML backend instances.
 *
 * @author Oracle
 */
@UtilityClass
public class MLBackendFactory {

    /**
     * Creates an ML backend of the specified type.
     *
     * @param backendType The type of backend to create
     * @param connection The connection handler (required for DBMS backend)
     * @return An instance of the requested backend
     * @throws IllegalArgumentException if backend type is null or unsupported
     */
    public static MLBackend createBackend(MLBackendType backendType, ConnectionHandler connection) {
        if (backendType == null) {
            throw new IllegalArgumentException("Backend type cannot be null");
        }

        return switch (backendType) {
            case TRIBUO -> new TribuoBackend();
            case DBMS_DATA_MINING -> {
                if (connection == null) {
                    throw new IllegalArgumentException(
                        "Connection handler is required for DBMS_DATA_MINING backend");
                }
                yield new DBMSBackend(connection);
            }
        };
    }
}
