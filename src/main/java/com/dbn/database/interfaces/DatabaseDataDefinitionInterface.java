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

package com.dbn.database.interfaces;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseObjectTypeId;
import com.dbn.editor.DBContentType;
import com.dbn.editor.code.content.SourceCodeContent;
import com.dbn.object.factory.MethodFactoryInput;
import com.intellij.openapi.project.Project;

import java.sql.SQLException;

public interface DatabaseDataDefinitionInterface extends DatabaseInterface{

    String createDDLStatement(Project project, DatabaseObjectTypeId objectTypeId, String userName, String schemaName, String objectName, DBContentType contentType, String code, String alternativeDelimiter);

    void computeSourceCodeOffsets(SourceCodeContent content, DatabaseObjectTypeId objectTypeId, String objectName);

    boolean includesTypeAndNameInSourceContent(DatabaseObjectTypeId objectTypeId);

    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    void createView(String viewName, String code, DBNConnection connection) throws SQLException;

    void createMethod(MethodFactoryInput methodFactoryInput, DBNConnection connection) throws SQLException;

    void createObject(String code, DBNConnection connection) throws SQLException;

    /*********************************************************
     *                   CHANGE statements                   *
     *********************************************************/
    void updateView(String viewName, String code, DBNConnection connection) throws SQLException;

    void updateTrigger(String tableOwner, String tableName, String triggerName, String oldCode, String newCode, DBNConnection connection) throws SQLException;

    @Deprecated // TODO add objectOwner / decommission schema connection context
    void updateObject(String objectName, String objectType, String oldCode, String newCode, DBNConnection connection) throws SQLException;


    default void updateJavaClass(String objectName, String newCode, DBNConnection connection) throws SQLException{};
   /*********************************************************
    *                   DROP statements                     *
    *********************************************************/
    void dropObject(String objectType, String objectName, DBNConnection connection) throws SQLException;

    void dropObjectBody(String objectType, String objectName, DBNConnection connection) throws SQLException;

    default void dropJavaClass(String ownerName, String objectName, DBNConnection connection) throws SQLException {}
   /*********************************************************
    *                   RENAME statements                     *
    *********************************************************/



}
