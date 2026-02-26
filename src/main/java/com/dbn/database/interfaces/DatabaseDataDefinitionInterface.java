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
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.type.DBConstraintType;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NonNls;

import java.sql.SQLException;

@NonNls
public interface DatabaseDataDefinitionInterface extends DatabaseInterface{

    String createDDLStatement(Project project, DatabaseObjectTypeId objectTypeId, String userName, String schemaName, String objectName, DBContentType contentType, String code, String alternativeDelimiter);

    void computeSourceCodeOffsets(SourceCodeContent content, DatabaseObjectTypeId objectTypeId, String objectName);

    boolean includesTypeAndNameInSourceContent(DatabaseObjectTypeId objectTypeId);

    String extractDDLStatement(String ownerName, String objectName, String objectType, DBNConnection connection) throws SQLException;

    /*********************************************************
     *                   CREATE statements                   *
     *********************************************************/
    void createView(String viewName, String code, DBNConnection connection) throws SQLException;

    void createMethod(DBObjectSpec methodSpec, DBNConnection connection) throws SQLException;

    void createTable(DBObjectSpec tableSpec, DBNConnection connection) throws SQLException;

    void createIndex(DBObjectSpec indexSpec, DBNConnection connection) throws SQLException;

    void createObject(String code, DBNConnection connection) throws SQLException;

    default void createJavaSource(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException {};

    /*********************************************************
     *                   UPDATE statements                   *
     *********************************************************/
    void updateView(String ownerName, String viewName, String code, boolean editionable, DBNConnection connection) throws SQLException;

    void updateJsonView(String ownerName, String viewName, String code, boolean editionable, DBNConnection connection) throws SQLException;

    void updateTrigger(String ownerName, String tableName, String triggerName, String oldCode, String newCode, DBNConnection connection) throws SQLException;

    void updateObject(String ownerName, String objectName, String objectType, String oldCode, String newCode, DBNConnection connection) throws SQLException;

    default void updateJavaSource(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException{};

    default void replaceJavaSource(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException{};

    default void replaceJavaClass(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException{};

    default void updateJavaResource(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException {}

    /*********************************************************
    *                   DROP statements                     *
    *********************************************************/
    void dropObject(String objectType, String ownerName, String objectName, DBNConnection connection) throws SQLException;

    void dropConstraint(String ownerName, String tableName, String constraintName, DBConstraintType constraintType, DBNConnection connection) throws SQLException;

    void dropObjectBody(String objectType, String ownerName, String objectName, DBNConnection connection) throws SQLException;

    default void dropJavaClass(String ownerName, String objectName, DBNConnection connection) throws SQLException {}

    /*********************************************************
     *                   COMPILE statements                  *
     *********************************************************/

    void compileObject(String ownerName, String objectName, String objectType, boolean debug, DBNConnection connection) throws SQLException;

    void compileObjectBody(String ownerName, String objectName, String objectType, boolean debug, DBNConnection connection) throws SQLException;

    void compileJavaClass(String ownerName, String objectName, DBNConnection connection) throws SQLException;

}
