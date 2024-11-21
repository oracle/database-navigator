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

package com.dbn.database.generic;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.database.common.DatabaseDataDefinitionInterfaceImpl;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.editor.DBContentType;
import com.dbn.object.factory.MethodFactoryInput;
import com.intellij.openapi.project.Project;

import java.sql.SQLException;

public class GenericDataDefinitionInterface extends DatabaseDataDefinitionInterfaceImpl {
    public GenericDataDefinitionInterface(DatabaseInterfaces provider) {
        super("generic_ddl_interface.xml", provider);
    }

    @Override
    public String createDDLStatement(Project project, DatabaseObjectTypeId objectTypeId, String userName, String schemaName, String objectName, DBContentType contentType, String code, String alternativeDelimiter) {
        return objectTypeId == DatabaseObjectTypeId.VIEW ? "create view " + objectName + " as\n" + code :
                objectTypeId == DatabaseObjectTypeId.FUNCTION ? "create function " + objectName + " as\n" + code :
                        "create or replace\n" + code;
    }

    public String getSessionSqlMode(DBNConnection connection) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    public void setSessionSqlMode(String sqlMode, DBNConnection connection) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /*********************************************************
     *                   CHANGE statements                   *
     *********************************************************/
    @Override
    public void updateView(String viewName, String code, DBNConnection connection) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void updateTrigger(String tableOwner, String tableName, String triggerName, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void updateObject(String objectName, String objectType, String oldCode, String newCode, DBNConnection connection) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

    /*********************************************************
     *                     DROP statements                   *
     *********************************************************/
    private void dropObjectIfExists(String objectType, String objectName, DBNConnection connection) throws SQLException {
        executeQuery(connection, true, "drop-object-if-exists", objectType, objectName);
    }

    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    @Override
    public void createMethod(MethodFactoryInput method, DBNConnection connection) throws SQLException {
        throw new UnsupportedOperationException("Not implemented");
    }

}
