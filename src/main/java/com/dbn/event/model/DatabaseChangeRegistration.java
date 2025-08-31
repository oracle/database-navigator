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

package com.dbn.event.model;

import com.dbn.common.reflection.ProxyObject;
import com.dbn.common.reflection.ProxyObjectInfo;
import com.dbn.common.util.Strings;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * proxy of {@link oracle.jdbc.dcn.DatabaseChangeRegistration}
 */
@ProxyObjectInfo(delegateClass = "oracle.jdbc.dcn.DatabaseChangeRegistration")
public interface DatabaseChangeRegistration extends ProxyObject {

    long getRegId();

    String[] getTables();

    void addListener(DatabaseChangeListener listener) throws SQLException;

    void addListener(DatabaseChangeListener listener, Executor executor) throws SQLException;

    void removeListener(DatabaseChangeListener listener) throws SQLException;

    default boolean matches(String tableIdentifier) {
        return Arrays.stream(getTables()).anyMatch(t -> Strings.containsIgnoreCase(t, tableIdentifier));
    }
}
