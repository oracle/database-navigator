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

package com.dbn.database.common.metadata;

import org.jetbrains.annotations.NonNls;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Objects;

public abstract class DBObjectMetadataBase {
    protected ResultSet resultSet;

    public DBObjectMetadataBase(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    @NonNls
    protected String getString(@NonNls String columnLabel) throws SQLException {
        String string = resultSet.getString(columnLabel);
        return string == null ? null : string.intern();
    }

    protected boolean isYesFlag(@NonNls String columnLabel) throws SQLException {
        return Objects.equals(getString(columnLabel), "Y");
    }
}
