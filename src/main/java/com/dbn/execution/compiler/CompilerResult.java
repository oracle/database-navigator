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

package com.dbn.execution.compiler;

import com.dbn.common.message.MessageType;
import com.dbn.common.notification.NotificationSupport;
import com.dbn.common.util.Naming;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.Resources;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.editor.DBContentType;
import com.dbn.nls.NlsSupport;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dbn.common.Priority.HIGH;
import static com.dbn.common.notification.NotificationGroup.COMPILER;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Getter
public class CompilerResult implements Disposable, NotificationSupport, NlsSupport {
    private final DBObjectRef<DBSchemaObject> object;
    private final List<CompilerMessage> compilerMessages = new ArrayList<>();
    private CompilerAction compilerAction;
    private boolean error = false;

    public CompilerResult(CompilerAction compilerAction, ConnectionHandler connection, DBSchema schema, DBObjectType objectType, String objectName, @Nullable DBNConnection conn) {
        object = new DBObjectRef<>(schema.ref(), objectType, objectName);
        init(connection, schema, objectName, objectType, compilerAction, conn);
    }

    public CompilerResult(CompilerAction compilerAction, DBSchemaObject object, @Nullable DBNConnection conn) {
        this.object = DBObjectRef.of(object);
        init(object.getConnection(), object.getSchema(), object.getName(), object.getObjectType(), compilerAction, conn);
    }

    public CompilerResult(CompilerAction compilerAction, DBSchemaObject object, DBContentType contentType, String errorMessage) {
        this.compilerAction = compilerAction;
        this.object = DBObjectRef.of(object);
        CompilerMessage compilerMessage = new CompilerMessage(this, contentType, errorMessage, MessageType.ERROR);
        compilerMessages.add(compilerMessage);
    }

    private void init(ConnectionHandler connection, DBSchema schema, String objectName, DBObjectType objectType, CompilerAction compilerAction, @Nullable DBNConnection conn) {
        this.compilerAction = compilerAction;
        DBContentType contentType = compilerAction.getContentType();
        String qualifiedObjectName = Naming.getQualifiedObjectName(objectType, objectName, schema);

        try {
            if (conn == null) {
                DatabaseInterfaceInvoker.execute(HIGH,
                        txt("prc.compiler.title.LoadingCompilerData"),
                        txt("prc.compiler.text.LoadingCompilerData", qualifiedObjectName),
                        connection.getProject(),
                        connection.getConnectionId(),
                        c -> loadCompilerErrors(connection, schema, objectName, contentType, c));
            } else {
                // already inside an interface thread
                loadCompilerErrors(connection, schema, objectName, contentType, conn);
            }
        } catch (SQLException e) {
            conditionallyLog(e);
            sendErrorNotification(COMPILER, txt("ntf.compiler.error.FailedToLoadCompilerResult", e));
        }


        if (compilerMessages.isEmpty()) {
            // TODO NLS
            String contentDesc =
                    contentType == DBContentType.CODE_SPEC ? "spec of " :
                    contentType == DBContentType.CODE_BODY ? "body of " : "";

            String message = "The " + contentDesc + object.getQualifiedNameWithType() + " was " + (compilerAction.isSave() ? "updated" : "compiled") + " successfully.";
            CompilerMessage compilerMessage = new CompilerMessage(this, contentType, message);
            compilerMessages.add(compilerMessage);
        } else {
            Collections.sort(compilerMessages);
        }
    }

    private void loadCompilerErrors(ConnectionHandler connection, DBSchema schema, String objectName, DBContentType contentType, DBNConnection conn) throws SQLException {
        ResultSet resultSet = null;
        try {
            DatabaseMetadataInterface metadata = connection.getMetadataInterface();
            resultSet = metadata.loadCompileObjectErrors(
                    schema.getName(),
                    objectName,
                    conn);

            while (resultSet != null && resultSet.next()) {
                CompilerMessage errorMessage = new CompilerMessage(this, resultSet);
                error = true;
                if (/*!compilerAction.isDDL() || */contentType.isBundle() || contentType == errorMessage.getContentType()) {
                    compilerMessages.add(errorMessage);
                }
            }
        } finally {
            Resources.close(resultSet);
        }
    }

    public boolean isSingleMessage() {
        return compilerMessages.size() == 1;
    }

    @Nullable
    public DBSchemaObject getObject() {
        return DBObjectRef.get(object);
    }

    public ConnectionId getConnectionId() {
        return object.getConnectionId();
    }

    DBObjectType getObjectType() {
        return object.getObjectType();
    }

    public DBObjectRef<DBSchemaObject> getObjectRef() {
        return object;
    }

    @Override
    public void dispose() {
        compilerMessages.clear();
    }

    public Project getProject() {
        DBSchemaObject object = DBObjectRef.get(this.object);
        if (object == null) {
            ConnectionHandler connection = this.object.getConnection();
            if (connection != null) return connection.getProject();
        } else {
            return object.getProject();
        }
        return null;
    }

    public boolean hasErrors() {
        for (CompilerMessage compilerMessage : compilerMessages) {
            if (compilerMessage.getType() == MessageType.ERROR) {
                return true;
            }
        }
        return false;
    }
}
