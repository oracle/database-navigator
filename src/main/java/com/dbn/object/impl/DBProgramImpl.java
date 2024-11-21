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

package com.dbn.object.impl;

import com.dbn.database.common.metadata.def.DBProgramMetadata;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBFunction;
import com.dbn.object.DBMethod;
import com.dbn.object.DBProcedure;
import com.dbn.object.DBProgram;
import com.dbn.object.DBSchema;
import com.dbn.object.DBType;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.DBSchemaObjectImpl;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import static com.dbn.object.common.property.DBObjectProperty.COMPILABLE;
import static com.dbn.object.common.property.DBObjectProperty.DEBUGABLE;
import static com.dbn.object.common.property.DBObjectProperty.INVALIDABLE;

abstract class DBProgramImpl<M extends DBProgramMetadata, P extends DBProcedure, F extends DBFunction, T extends DBType>
        extends DBSchemaObjectImpl<M> implements DBProgram<P, F, T> {

    DBProgramImpl(DBSchemaObject parent, M metadata) throws SQLException {
        super(parent, metadata);
    }

    DBProgramImpl(DBSchema schema, M metadata) throws SQLException {
        super(schema, metadata);
    }

    @Override
    public void initProperties() {
        super.initProperties();
        properties.set(INVALIDABLE, true);
        properties.set(COMPILABLE, true);
        properties.set(DEBUGABLE, true);
    }

    @Override
    public void initStatus(M metadata) throws SQLException {
        String specValidString = metadata.getSpecValid();
        String bodyValidString = metadata.getBodyValid();

        String specDebugString = metadata.getSpecDebug();
        String bodyDebugString = metadata.getBodyDebug();

        DBObjectStatusHolder objectStatus = getStatus();

        boolean specPresent = specValidString != null;
        boolean specValid = !specPresent || Objects.equals(specValidString, "Y");
        boolean specDebug = !specPresent || Objects.equals(specDebugString, "Y");

        boolean bodyPresent = bodyValidString != null;
        boolean bodyValid = !bodyPresent || Objects.equals(bodyValidString, "Y");
        boolean bodyDebug = !bodyPresent || Objects.equals(bodyDebugString, "Y");

        objectStatus.set(DBContentType.CODE_SPEC, DBObjectStatus.PRESENT, specPresent);
        objectStatus.set(DBContentType.CODE_SPEC, DBObjectStatus.VALID, specValid);
        objectStatus.set(DBContentType.CODE_SPEC, DBObjectStatus.DEBUG, specDebug);

        objectStatus.set(DBContentType.CODE_BODY, DBObjectStatus.PRESENT, bodyPresent);
        objectStatus.set(DBContentType.CODE_BODY, DBObjectStatus.VALID, bodyValid);
        objectStatus.set(DBContentType.CODE_BODY, DBObjectStatus.DEBUG, bodyDebug);

    }

    protected abstract DBObjectType getFunctionObjectType();
    protected abstract DBObjectType getProcedureObjectType();
    protected abstract DBObjectType getTypeObjectType();

    @Override
    public List<F> getFunctions() {
        return getChildObjects(getFunctionObjectType());
    }

    @Override
    public List<P> getProcedures() {
        return getChildObjects(getProcedureObjectType());
    }

    @Override
    public List<T> getTypes() {
        return getChildObjects(getTypeObjectType());
    }

    @Override
    public F getFunction(String name, short overload) {
        return getChildObject(getFunctionObjectType(), name, overload);
    }

    @Override
    public P getProcedure(String name, short overload) {
        return getChildObject(getProcedureObjectType(), name, overload);
    }

    @Override
    public T getType(String name) {
        return getChildObject(getTypeObjectType(), name);
    }

    @Override
    public DBMethod getMethod(String name, short overload) {
        DBMethod method = getProcedure(name, overload);
        if (method == null) method = getFunction(name, overload);
        return method;
    }

    @Override
    public boolean isEmbedded() {
        return false;
    }

    @Override
    public boolean isEditable(DBContentType contentType) {
        return getContentType() == DBContentType.CODE_SPEC_AND_BODY && (
                contentType == DBContentType.CODE_SPEC ||
                contentType == DBContentType.CODE_BODY);
    }
}
