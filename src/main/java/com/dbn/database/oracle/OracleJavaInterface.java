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

package com.dbn.database.oracle;

import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.common.DatabaseInterfaceBase;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.database.interfaces.DatabaseJavaInterface;

import java.sql.SQLException;

import static com.dbn.common.util.Naming.unquote;

public class OracleJavaInterface extends DatabaseInterfaceBase implements DatabaseJavaInterface {
    public OracleJavaInterface(DatabaseInterfaces provider) {
        super("oracle_java_interface.xml", provider);
    }

    @Override
    public void createJavaSource(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "prepare-java-staging-table", ownerName);
        executeUpdate(connection, "create-java-source", ownerName, objectName, content);
        compileJavaClass(ownerName, objectName, connection);
    }

    @Override
    public void updateJavaSource(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "prepare-java-staging-table", ownerName);
        executeUpdate(connection, "update-java-source", ownerName, objectName, content);
        compileJavaClass(ownerName, objectName, connection);
    }

    @Override
    public void replaceJavaSource(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException {
        dropJavaClass(ownerName, objectName, connection);
        executeUpdate(connection, "prepare-java-staging-table", ownerName);
        executeUpdate(connection, "create-java-source", ownerName, objectName, content);
    }

    @Override
    public void replaceJavaClass(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException {
        dropJavaClass(ownerName, objectName, connection);
        executeUpdate(connection, "prepare-java-staging-table", ownerName);
        executeUpdate(connection, "create-java-class", ownerName, objectName, content);
    }

    @Override
    public void updateJavaResource(String ownerName, String objectName, byte[] content, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "prepare-java-staging-table", ownerName);
        executeUpdate(connection, "update-java-resource", ownerName, objectName, content);
    }

    @Override
    public void dropJavaClass(String ownerName, String objectName, DBNConnection connection) throws SQLException {
        executeUpdate(connection, "drop-java-source", ownerName, objectName);
        executeUpdate(connection, "drop-java-class", ownerName, objectName);
    }

    @Override
    public void compileJavaClass(String ownerName, String objectName, DBNConnection connection) throws SQLException {
        try {
            executeSilentUpdate(connection, "set-java-property", "sun.tools.javac.Main.args", 'g');
            executeSilentUpdate(connection, "set-java-compiler-option", unquote(objectName), "debug", "true");
            executeUpdate(connection, "compile-java-source", ownerName, objectName);
            executeUpdate(connection, "compile-java-class", ownerName, objectName);
        } finally {
            executeSilentUpdate(connection, "set-java-compiler-option", unquote(objectName), "debug", "false");
        }
    }
}
