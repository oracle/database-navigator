/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.event.notification.model;

import com.dbn.common.util.Naming;
import com.dbn.connection.ConnectionId;
import com.dbn.object.DBDataset;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Data;

import static com.dbn.object.type.DBObjectType.SCHEMA;
import static com.dbn.object.type.DBObjectType.TABLE;

@Data
public class DataChangeNotification implements Comparable<DataChangeNotification>{
    private long timestamp = System.currentTimeMillis();

    private ConnectionId connectionId;
    private Long regId;
    private String operation;
    private String rowId;
    private String tableIdentifier;
    private DBObjectRef<DBDataset> table;


    public DataChangeNotification(String operation, String tableIdentifier, String rowId, Long regId, ConnectionId connectionId) {
        this.connectionId = connectionId;
        this.tableIdentifier = tableIdentifier;
        this.operation = operation;
        this.rowId = rowId;
        this.regId = regId;

        this.table = initTable(tableIdentifier);
    }

    private DBObjectRef<DBDataset> initTable(String tableIdentifier) {
        String[] tokens = tableIdentifier.split("\\.");
        String schemaName = Naming.unquote(tokens[0]);
        String tableName = Naming.unquote(tokens[1]);

        DBObjectRef<DBSchema> schema = new DBObjectRef<>(connectionId, SCHEMA, schemaName);
        return new DBObjectRef<>(schema, TABLE, tableName);
    }

    public boolean matches(DBDataset dataset) {
        return table.equals(dataset.ref());
    }

    @Override
    public int compareTo(DataChangeNotification o) {
        return Long.compare(timestamp, o.timestamp);
    }

    public boolean isAfter(long timestamp) {
        return this.timestamp > timestamp;
    }
}
