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

import java.sql.SQLException;
import java.util.Properties;

/**
 * Proxy of {@link oracle.jdbc.OracleConnection}
 */
@ProxyObjectInfo(delegateClass = "oracle.jdbc.OracleConnection")
public interface OracleConnection extends ProxyObject {

    DatabaseChangeRegistration registerDatabaseChangeNotification(Properties properties) throws SQLException;

    void unregisterDatabaseChangeNotification(DatabaseChangeRegistration registration) throws SQLException;

    void unregisterDatabaseChangeNotification(int regId) throws SQLException;

    void unregisterDatabaseChangeNotification(long regId, String callback) throws SQLException;

    OracleStatement createStatement() throws SQLException;
}
