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

package com.dbn.data.type;

import com.dbn.common.dispose.Disposer;
import com.dbn.common.dispose.StatefulDisposableBase;
import com.dbn.common.dispose.UnlistedDisposable;
import com.dbn.common.latent.Latent;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.ConnectionRef;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.object.DBColumn;
import com.dbn.object.DBNestedTable;
import com.dbn.object.DBProgram;
import com.dbn.object.DBSchema;
import com.dbn.object.DBTable;
import com.dbn.object.DBType;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.lookup.DBObjectRef;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.object.type.DBObjectType.COLUMN;
import static com.dbn.object.type.DBObjectType.NESTED_TABLE;
import static com.dbn.object.type.DBObjectType.PROGRAM;
import static com.dbn.object.type.DBObjectType.SCHEMA;
import static com.dbn.object.type.DBObjectType.TABLE;
import static com.dbn.object.type.DBObjectType.TYPE;

public final class DBDataTypeBundle extends StatefulDisposableBase implements UnlistedDisposable {
    private final ConnectionRef connection;

    private final Latent<Map<String, DBNativeDataType>> nativeDataTypes = Latent.basic(() -> createNativeDataTypes());
    private final Map<DBDataTypeDefinition, DBDataType> dataTypes = new ConcurrentHashMap<>();

    public DBDataTypeBundle(@NotNull ConnectionHandler connection) {
        this.connection = connection.ref();
    }

    @NotNull
    public ConnectionHandler getConnection() {
        return ConnectionRef.ensure(connection);
    }

    private Map<String, DBNativeDataType> getNativeDataTypes() {
        checkDisposed();
        return nativeDataTypes.get();
    }

    public DBNativeDataType getNativeDataType(String name) {
        if (name == null) return null;
        if (name.isBlank()) return null;

        String upperCaseName = cachedUpperCase(name);
        Map<String, DBNativeDataType> dataTypes = getNativeDataTypes();

        DBNativeDataType dataType = dataTypes.get(upperCaseName);
        if (dataType != null) return dataType;

        for (var entry : dataTypes.entrySet()) {
            String key = entry.getKey();
            DBNativeDataType value = entry.getValue();
            if (key.startsWith(upperCaseName)) {
                return value;
            }
        }
        return null;
    }


    private Map<String, DBNativeDataType> createNativeDataTypes() {
        Map<String, DBNativeDataType> nativeDataTypes = new HashMap<>();

        DatabaseInterfaces interfaces = getConnection().getInterfaces();
        List<DataTypeDefinition> dataTypeDefinitions = interfaces.getNativeDataTypes().list();
        for (DataTypeDefinition dataTypeDefinition : dataTypeDefinitions) {
            DBNativeDataType dataType = new DBNativeDataType(dataTypeDefinition);
            nativeDataTypes.put(cachedUpperCase(dataType.getName()), dataType);
        }
        return nativeDataTypes;
    }

    public DBDataType getDataType(DBDataTypeDefinition definition) {
        return dataTypes.computeIfAbsent(definition, d -> createDataType(d));
    }

    private DBDataType createDataType(DBDataTypeDefinition def) {
        checkDisposed();
        String name = null;
        DBObjectRef<DBType> declaredType = null;
        DBNativeDataType nativeDataType = null;

        DBObjectBundle objectBundle = getConnection().getObjectBundle();
        String declaredTypeOwner = def.getDeclaredTypeOwner();
        String declaredTypeProgram = def.getDeclaredTypeProgram();
        String declaredTypeName = def.getDeclaredTypeName();
        String dataTypeName = def.getDataTypeName();

        ConnectionId connectionId = getConnection().getConnectionId();
        if (declaredTypeOwner != null) {
            DBObjectRef<DBSchema> schema = new DBObjectRef<>(connectionId, SCHEMA, declaredTypeOwner);
            if (declaredTypeProgram != null) {
                DBObjectRef<DBProgram> program = new DBObjectRef<>(schema, PROGRAM, declaredTypeProgram);
                declaredType = new DBObjectRef<>(program, TYPE, declaredTypeName);
            } else {
                declaredType = new DBObjectRef<>(schema, TYPE, declaredTypeName);
            }

            name = declaredTypeName;
            DBNativeDataType nDataType = objectBundle.getNativeDataType(dataTypeName);
            if (nDataType != null && nDataType.getDefinition().isPseudoNative()) {
                nativeDataType = nDataType;
            }

        } else {
            nativeDataType = objectBundle.getNativeDataType(dataTypeName);
            if (nativeDataType == null) name = dataTypeName;
        }


        DBObjectRef<DBNestedTable> nestedTable = null;
        DBObjectRef<DBColumn> nestedTableColumn = null;
        String nestedTableName = def.getNestedTableName();
        if (nestedTableName != null) {
            DBObjectRef<DBSchema> schema = new DBObjectRef<>(connectionId, SCHEMA, def.getNestedTableParentOwnerName());
            DBObjectRef<DBTable> table = new DBObjectRef<>(schema, TABLE, def.getNestedTableParentTableName());
            nestedTable = new DBObjectRef<>(table, NESTED_TABLE, nestedTableName);
            nestedTableColumn = new DBObjectRef<>(table, COLUMN, def.getNestedTableParentColumnName());
        }

        DBDataType dataType = new DBDataType();
        dataType.setNativeType(nativeDataType);
        dataType.setDeclaredType(declaredType);
        dataType.setNestedTable(nestedTable);
        dataType.setNestedTableColumn(nestedTableColumn);

        dataType.setName(name);
        dataType.setLength(def.getLength());
        dataType.setPrecision(def.getPrecision());
        dataType.setScale(def.getScale());
        dataType.setSet(def.isSet());
        dataType.setCollection(nativeDataType == null ? def.isCollection() : nativeDataType.isCollection());
        dataType.setTable(nestedTable != null);

        return dataType;
    }


    @Override
    public void disposeInner() {
        nativeDataTypes.reset();
        Disposer.dispose(dataTypes);
    }
}
