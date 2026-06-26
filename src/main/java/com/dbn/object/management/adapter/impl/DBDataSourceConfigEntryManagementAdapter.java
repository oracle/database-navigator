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

package com.dbn.object.management.adapter.impl;

import com.dbn.object.DBDataSourceConfigEntry;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.management.ObjectManagementAdapter;
import com.dbn.object.management.ObjectManagementAdapterExtension;
import com.dbn.object.management.adapter.DBObjectCreateAdapter;
import com.dbn.object.management.adapter.DBObjectDeleteAdapter;
import com.dbn.object.type.DBObjectType;

import static com.dbn.common.constant.Constant.array;
import static com.dbn.common.exception.Exceptions.unsupported;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.event.ObjectChangeAction.DELETE;
import static com.dbn.object.type.DBObjectType.DATA_SOURCE_CONFIG_ENTRY;

public class DBDataSourceConfigEntryManagementAdapter implements ObjectManagementAdapterExtension<DBDataSourceConfigEntry> {
    @Override
    public DBObjectType[] getObjectTypes() {
        return array(DATA_SOURCE_CONFIG_ENTRY);
    }

    @Override
    public ObjectManagementAdapter<DBDataSourceConfigEntry> createAdapter(DBDataSourceConfigEntry object, ObjectChangeAction action) {
        if (action == CREATE) {
            return new DBObjectCreateAdapter<>(
                    object,
                    (connection, conn, entry) -> connection.getDataSourceConfigInterface().insertDataSourceConfigEntry(
                            entry.getName(),
                            entry.getValue(),
                            conn));
        }

        if (action == DELETE) {
            return new DBObjectDeleteAdapter<>(
                    object,
                    (connection, conn, entry) -> connection.getDataSourceConfigInterface().deleteDataSourceConfigEntry(entry.getName(), conn));
        }

        return unsupported(action);
    }
}
