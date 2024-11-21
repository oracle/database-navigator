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

package com.dbn.database.postgres;

import com.dbn.common.util.Strings;
import com.dbn.database.DatabaseObjectIdentifier;
import com.dbn.database.interfaces.DatabaseMessageParserInterface;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.sql.SQLTimeoutException;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public class PostgresMessageParserInterface implements DatabaseMessageParserInterface {

    @Override
    @Nullable
    public DatabaseObjectIdentifier identifyObject(SQLException exception) {
         return null;
    }

    @Override
    public boolean isTimeoutException(SQLException e) {
        return e instanceof SQLTimeoutException;
    }

    @Override
    public boolean isModelException(SQLException e) {
        String sqlState = getSqlState(e);
        return Strings.isOneOfIgnoreCase(sqlState, "3D000", "3F000", "42P01", "42703", "42704");
    }

    @Override
    public boolean isAuthenticationException(SQLException e) {
        String sqlState = getSqlState(e);
        return Strings.isOneOfIgnoreCase(sqlState, "28P01");
    }

    private static String getSqlState(SQLException e) {
        try {
            Method method = e.getClass().getMethod("getSQLState");
            return (String) method.invoke(e);
        } catch (Exception ex) {
            conditionallyLog(ex);
            log.error("Could not get exception SQLState", ex);
        }
        return "";
    }

    @Override
    public boolean isSuccessException(SQLException exception) {
        return false;
    }
}