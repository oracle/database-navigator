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

package com.dbn.database.common.debug;

import com.dbn.database.common.statement.CallableStatementOutput;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;


public class DebuggerVersionInfo implements CallableStatementOutput {
    private String version;


    public String getVersion() {
        return version;
    }

    @Override
    public void registerParameters(CallableStatement statement) throws SQLException {
        statement.registerOutParameter(1, Types.VARCHAR);
    }

    @Override
    public void read(CallableStatement statement) throws SQLException {
        version = statement.getString(1);
    }
}
