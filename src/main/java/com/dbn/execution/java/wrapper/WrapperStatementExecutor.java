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

package com.dbn.execution.java.wrapper;

import com.dbn.common.Priority;
import com.dbn.common.event.ProjectEvents;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.jdbc.DBNPreparedStatement;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.common.DBObject;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import lombok.Getter;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.object.event.ObjectChangeAction.CREATE;

@Getter
public class WrapperStatementExecutor {
    private Wrapper wrapper;

    public Wrapper createExecutionWrappers(DBJavaMethod method, boolean useFriendlyNames, boolean compileInDebugMode) throws SQLException {
        Project project = method.getProject();

        WrapperBuilder wrapperBuilder = WrapperBuilder.getInstance();
        wrapper = wrapperBuilder.build(method, useFriendlyNames);

        WrapperStatementBuilder statementBuilder = new WrapperStatementBuilder(project);
        String creationStatement = statementBuilder.buildWrapperCreationStatement(wrapper);

        ConnectionId connectionId = method.getConnectionId();
        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Creating execution wrappers",
                "Creating java execution wrappers for method \"" + method.getPresentableText() + "\"",
                project,
                connectionId, c -> {
                    DBNPreparedStatement statement = c.prepareStatement(creationStatement);
                    statement.execute();
                    if(compileInDebugMode) {
                        compileObjectInDebugMode(c, method.getConnection(), method.getSchemaName(), wrapper);
                    }
                });

        if (useFriendlyNames) {
            notifyObjectChanges(method, DBObjectType.JAVA_CLASS, CREATE);
            notifyObjectChanges(method, DBObjectType.FUNCTION, CREATE);
            notifyObjectChanges(method, DBObjectType.PROCEDURE, CREATE);
            notifyObjectChanges(method, DBObjectType.TYPE, CREATE);
        }

        return wrapper;
    }

    public Wrapper createExecutionWrappers(DBJavaClass javaClass, List<DBJavaMethod> methods, boolean useFriendlyNames, boolean compileInDebugMode) throws SQLException {
        if (methods.isEmpty()) return null;

        Project project = javaClass.getProject();
        WrapperBuilder wrapperBuilder = WrapperBuilder.getInstance();
        Wrapper wrapper = wrapperBuilder.build(javaClass, methods, useFriendlyNames);

        WrapperStatementBuilder statementBuilder = new WrapperStatementBuilder(project);
        String creationStatement = statementBuilder.buildWrapperCreationStatement(wrapper);

        ConnectionId connectionId = javaClass.getConnectionId();
        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Creating execution wrappers",
                "Creating java execution wrappers for java class \"" + javaClass.getCanonicalName() + "\"",
                project,
                connectionId, c -> {
                    DBNPreparedStatement statement = c.prepareStatement(creationStatement);
                    statement.execute();
                    if(compileInDebugMode) {
                        compileObjectInDebugMode(c, javaClass.getConnection(), javaClass.getSchemaName(), wrapper);
                    }
                });

        if (useFriendlyNames) {
            notifyObjectChanges(javaClass, DBObjectType.JAVA_CLASS, CREATE);
            notifyObjectChanges(javaClass, DBObjectType.PACKAGE, CREATE);
            notifyObjectChanges(javaClass, DBObjectType.TYPE, CREATE);
        }

        return wrapper;
    }

    public void discardExecutionWrappers(DBJavaMethod method) throws SQLException {
        Project project = method.getProject();
        WrapperStatementBuilder statementBuilder = new WrapperStatementBuilder(project);
        String removalStatement = statementBuilder.buildWrapperRemovalStatement(wrapper);

        ConnectionId connectionId = method.getConnectionId();
        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Removing execution wrappers",
                "Removing java execution wrappers for " + method.getPresentableText(),
                project,
                connectionId, c -> {
                    DBNPreparedStatement statement = c.prepareStatement(removalStatement);
                    statement.execute();
                });
    }

    public void notifyObjectChanges(DBObject sourceObject, DBObjectType objectType, ObjectChangeAction action) {
        Project project = sourceObject.getProject();
        ConnectionId connectionId = sourceObject.getConnectionId();
        SchemaId schemaId = sourceObject.getSchemaId();
        ProjectEvents.notify(project, ObjectChangeListener.TOPIC, l -> l.objectsChanged(connectionId, schemaId, objectType, action));
    }

    private void compileObjectInDebugMode(DBNConnection connection, ConnectionHandler connectionHandler, String schemaName, Wrapper wrapper) throws SQLException{
        DatabaseDataDefinitionInterface dataDefinitionInterface = connectionHandler.getDataDefinitionInterface();

        for(String typeName: wrapper.getSqlTypeNames())
            dataDefinitionInterface.compileObject(schemaName, typeName, "TYPE", true, connection);

        String objectType;
        if(wrapper.getSqlWrapperMethod().getObjectType() == DBObjectType.PROCEDURE) {
            objectType = "PROCEDURE";
        } else {
            objectType = "FUNCTION";
        }
        String sqlWrapperName = wrapper.getSqlWrapperName();

        dataDefinitionInterface.compileObject(schemaName, sqlWrapperName, objectType, true, connection);
    }
}
