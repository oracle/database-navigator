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

package com.dbn.database.common.metadata.impl;

import com.dbn.database.common.metadata.DBObjectMetadataBase;
import com.dbn.database.common.metadata.def.DBMethodMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public abstract class DBMethodMetadataImpl extends DBObjectMetadataBase implements DBMethodMetadata {
    DBMethodMetadataImpl(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public boolean isDeterministic() throws SQLException {
        return isYesFlag("IS_DETERMINISTIC");
    }

    @Override
    public boolean isValid() throws SQLException {
        return isYesFlag("IS_VALID");
    }

    @Override
    public boolean isDebug() throws SQLException {
        return isYesFlag("IS_DEBUG");
    }

    @Override
    public short getOverload() throws SQLException {
        return resultSet.getShort("OVERLOAD");
    }

    @Override
    public short getPosition() throws SQLException {
        return resultSet.getShort("POSITION");
    }

    @Override
    public String getLanguage() throws SQLException {
        return getString("LANGUAGE");
    }

    @Override
    public String getTypeName() throws SQLException {
        return getString("TYPE_NAME");
    }

    @Override
    public String getPackageName() throws SQLException {
        return getString("PACKAGE_NAME");
    }
}
