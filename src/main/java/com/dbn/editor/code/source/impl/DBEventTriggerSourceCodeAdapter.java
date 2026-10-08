/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.editor.code.source.impl;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBEventTrigger;

import java.sql.ResultSet;
import java.sql.SQLException;

import static com.dbn.object.type.DBObjectType.EVENT_TRIGGER;

public class DBEventTriggerSourceCodeAdapter extends DBMetadataSourceCodeAdapter<DBEventTrigger> {
    public DBEventTriggerSourceCodeAdapter() {
        super(EVENT_TRIGGER);
    }

    @Override
    public ResultSet loadSourceCode(DBEventTrigger trigger, DBContentType contentType, DBNConnection connection) throws SQLException {
        DatabaseMetadataInterface metadataInterface = trigger.getMetadataInterface();
        return metadataInterface.loadEventTriggerSourceCode(trigger.getName(), connection);
    }
}
