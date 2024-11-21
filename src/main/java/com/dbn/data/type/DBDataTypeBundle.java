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
import com.dbn.connection.ConnectionRef;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.object.DBProgram;
import com.dbn.object.DBSchema;
import com.dbn.object.DBType;
import com.dbn.object.common.DBObjectBundle;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import lombok.val;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.dbn.common.util.Strings.cachedUpperCase;

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

        for (val entry : dataTypes.entrySet()) {
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

        if (declaredTypeOwner != null) {
            DBObjectRef<DBSchema> schema = new DBObjectRef<>(getConnection().getConnectionId(), DBObjectType.SCHEMA, declaredTypeOwner);
            if (declaredTypeProgram != null) {
                DBObjectRef<DBProgram> program = new DBObjectRef<>(schema, DBObjectType.PROGRAM, declaredTypeProgram);
                declaredType = new DBObjectRef<>(program, DBObjectType.TYPE, declaredTypeName);
            } else {
                declaredType = new DBObjectRef<>(schema, DBObjectType.TYPE, declaredTypeName);
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

        DBDataType dataType = new DBDataType();
        dataType.setNativeType(nativeDataType);
        dataType.setDeclaredType(declaredType);
        dataType.setName(name);
        dataType.setLength(def.getLength());
        dataType.setPrecision(def.getPrecision());
        dataType.setScale(def.getScale());
        dataType.setSet(def.isSet());
        return dataType;
    }


    @Override
    public void disposeInner() {
        nativeDataTypes.reset();
        Disposer.dispose(dataTypes);
    }
}
