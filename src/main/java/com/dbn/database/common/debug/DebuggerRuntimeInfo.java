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

import lombok.Getter;
import lombok.Setter;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Objects;

@Getter
@Setter
public class DebuggerRuntimeInfo extends BasicOperationInfo {
    private String ownerName;
    private String programName;
    private Integer namespace;
    private Integer lineNumber;
    private Integer frameIndex;
    private int breakpointId;
    private int reason;
    private boolean terminated;

    public DebuggerRuntimeInfo() {}

    public DebuggerRuntimeInfo(String ownerName, String programName, Integer namespace, Integer lineNumber) {
        this.ownerName = ownerName;
        this.programName = programName;
        this.namespace = namespace;
        this.lineNumber = Math.max(lineNumber -1, 0) ;
    }

    @Override
    public void registerParameters(CallableStatement statement) throws SQLException {
        registerOutParameter(statement, 1, Types.VARCHAR);
        registerOutParameter(statement, 2, Types.VARCHAR);
        registerOutParameter(statement, 3, Types.NUMERIC);
        registerOutParameter(statement, 4, Types.NUMERIC);
        registerOutParameter(statement, 5, Types.NUMERIC);
        registerOutParameter(statement, 6, Types.NUMERIC);
        registerOutParameter(statement, 7, Types.NUMERIC);
        registerOutParameter(statement, 8, Types.VARCHAR);
    }

    @Override
    public void read(CallableStatement statement) throws SQLException {
        ownerName = getString(statement, 1);
        programName = getString(statement, 2);
        namespace = getInt(statement, 3);
        lineNumber = Math.max(getInt(statement, 4) - 1, 0);
        terminated = getInt(statement, 5) != 0;
        breakpointId = getInt(statement, 6);
        reason = getInt(statement, 7);
        error = getString(statement, 8);
    }

    public boolean isSameLocation(DebuggerRuntimeInfo runtimeInfo) {
        return
            Objects.equals(this.ownerName, runtimeInfo.ownerName) &&
            Objects.equals(this.programName, runtimeInfo.programName) &&
            Objects.equals(this.lineNumber, runtimeInfo.lineNumber);

    }
}
