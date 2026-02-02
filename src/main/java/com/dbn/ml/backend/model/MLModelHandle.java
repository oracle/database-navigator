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

package com.dbn.ml.backend.model;

import com.dbn.connection.ConnectionHandler;
import com.dbn.ml.backend.MLBackendType;
import com.dbn.ml.model.MLTaskType;

/**
 * Backend-agnostic handle to a trained ML model.
 * <p>
 * Implementations:
 * - TribuoModelHandle: Wraps Tribuo Model&lt;?&gt; (in-memory)
 * - DBMSModelHandle: Stores database model name and schema (database reference)
 *
 * @author ayoub allali
 */
public interface MLModelHandle {

    /**
     * Returns the backend type that created this model.
     */
    MLBackendType getBackendType();

    /**
     * Returns the ML task type (classification or regression).
     */
    MLTaskType getTaskType();

    /**
     * Returns the original backend-specific model object.
     * <p>
     * For Tribuo: org.tribuo.Model&lt;?&gt;
     * <br>
     * For DBMS: String modelName (reference)
     */
    Object getNativeModel();

    /**
     * Returns model metadata (feature names, output info, etc.)
     */
    MLModelMetadata getMetadata();

    /**
     * Returns the connection handler (if database-backed), null otherwise.
     */
    ConnectionHandler getConnection();
}
