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

package com.dbn.object.factory.adapter;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.security.DatabaseIdentifierCache;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.nls.NlsSupport;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.ObjectFactoryAdapter;
import com.dbn.object.factory.model.DBJavaClassFactoryInput;
import com.dbn.object.factory.ui.DBJavaClassFactoryInputForm;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;

import java.sql.SQLException;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.constant.Constant.array;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.type.DBObjectType.JAVA_CLASS;

public class DBJavaClassFactoryAdapter implements ObjectFactoryAdapter<DBJavaClassFactoryInput, DBJavaClassFactoryInputForm>, NlsSupport {
    private static final DBObjectType[] OBJECT_TYPES = array(JAVA_CLASS);

    @Override
    public DBObjectType[] getObjectTypes() {
        return OBJECT_TYPES;
    }

    public DBJavaClassFactoryInput createInput(DBSchema schema, DBObjectType objectType) {
        return new DBJavaClassFactoryInput(schema);
    }

    public DBJavaClassFactoryInputForm createInputForm(DBNComponent parent, DBJavaClassFactoryInput input) {
        return new DBJavaClassFactoryInputForm(parent, input);
    }

    @Override
    public void createObject(DBJavaClassFactoryInput input) throws SQLException {
        String className = input.getClassName();
        String packageName = input.getPackageName();
        String classType = input.getTypeIdentifier();
        String extendsSuffix = input.getExtendsSuffix();
        DBSchema schema = input.getSchema();

        StringBuilder javaCode = new StringBuilder();
        if(isNotEmpty(packageName)) {
            javaCode.append("package ").append(packageName).append(";").append("\n");
        }

        javaCode.append("public ").append(classType).append(" ").append(className).append(extendsSuffix)
                .append("{")
                .append("\n")
                .append("}");

        String objectName = input.getDatabaseObjectName();
        ConnectionId connectionId = schema.getConnectionId();
        SchemaId schemaId = schema.getSchemaId();

        DatabaseInterfaceInvoker.execute(HIGHEST,
                "Creating " + input.getObjectType().getTitleCasedName(),
                "Creating " + input.getObjectDescription(),
                schema.getProject(),
                connectionId,
                conn -> {
                    ConnectionHandler connection = schema.getConnection();
                    DatabaseDataDefinitionInterface dataDefinition = connection.getDataDefinitionInterface();
                    DatabaseIdentifierCache identifierCache = connection.getIdentifierCache();
                    String quotedObjectName = identifierCache.getQuotedIdentifier(objectName);
                    dataDefinition.createJavaSource(schema.getName(), quotedObjectName, javaCode.toString().getBytes(), conn);
                });

        ObjectChangeEvent.notify(CREATE, JAVA_CLASS, connectionId, schemaId);

        DBJavaClass javaClass = schema.getChildObject(JAVA_CLASS, objectName, false);
        if (javaClass == null) return;

        Project project = input.getProject();
        DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
        editorManager.connectAndOpenEditor(javaClass, null, false, true);
    }
}
