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
import com.dbn.database.common.metadata.def.DBTriggerMetadata;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DBTriggerMetadataImpl extends DBObjectMetadataBase implements DBTriggerMetadata {

    public DBTriggerMetadataImpl(ResultSet resultSet) {
        super(resultSet);
    }

    @Override
    public String getTriggerName() throws SQLException {
        return getString("TRIGGER_NAME");
    }

    @Override
    public String getDatasetName() throws SQLException {
        return getString("DATASET_NAME");
    }

    @Override
    public String getTriggerType() throws SQLException {
        return getString("TRIGGER_TYPE");
    }

    @Override
    public String getTriggeringEvent() throws SQLException {
        return getString("TRIGGERING_EVENT");
    }

    @Override
    public boolean isForEachRow() throws SQLException {
        return isYesFlag("IS_FOR_EACH_ROW");
    }

    @Override
    public boolean isEnabled() throws SQLException {
        return isYesFlag("IS_ENABLED");
    }

    @Override
    public boolean isValid() throws SQLException {
        return isYesFlag("IS_VALID");
    }

    @Override
    public boolean isDebug() throws SQLException {
        return isYesFlag("IS_DEBUG");
    }

}
