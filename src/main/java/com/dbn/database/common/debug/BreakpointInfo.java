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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
public class BreakpointInfo extends BasicOperationInfo {
    private Integer breakpointId;

    @Override
    public void registerParameters(CallableStatement statement) throws SQLException {
        statement.registerOutParameter(1, Types.NUMERIC);
        statement.registerOutParameter(2, Types.VARCHAR);
    }

    @Override
    public void read(CallableStatement statement) throws SQLException {
        Object object = statement.getObject(1);
        if (object instanceof BigDecimal) {
            BigDecimal bigDecimal = (BigDecimal) object;
            breakpointId = bigDecimal.intValue();
        }

        error = statement.getString(2);
    }
}
