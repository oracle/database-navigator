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

package com.dbn.database.common.util;

import com.dbn.common.data.Data;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Single value result set consumer, expecting the first column in the result set to be a boolean representation (Y, N, YES, NO, 1, 0)
 * See {@link Data#asBoolean(Object)}
 *
 *  * @author Dan Cioca (Oracle)
 */
public class BooleanResultSetConsumer extends ResultSetConsumer<Boolean> {
    public static final BooleanResultSetConsumer INSTANCE = new BooleanResultSetConsumer();
    private BooleanResultSetConsumer() {}

    @Override
    protected Boolean read(ResultSet resultSet) throws SQLException {
        if (resultSet.next()) {
            String value = resultSet.getString(1);
            return Data.asBoolean(value);
        }
        return false;
    }
}
