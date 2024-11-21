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
import com.dbn.database.common.metadata.def.DBCredentialMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBCredentialMetadataImpl extends DBObjectMetadataBase implements DBCredentialMetadata {

    public DBCredentialMetadataImpl(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public String getCredentialName() throws SQLException {
        return getString("CREDENTIAL_NAME");
    }

    public String getCredentialType() throws SQLException {
        return getString("CREDENTIAL_TYPE");
    }

    @Override
    public String getUserName() throws SQLException {
        return getString("USER_NAME");
    }

    @Override
    public String getComments() throws SQLException {
        return getString("COMMENTS");
    }

    @Override
    public boolean isEnabled() throws SQLException {
        return isYesFlag("IS_ENABLED");
    }
}
