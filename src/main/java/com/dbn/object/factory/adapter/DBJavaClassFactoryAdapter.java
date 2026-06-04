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
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseJavaInterface;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.ObjectFactoryAdapter;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.ui.DBJavaClassFactoryInputForm;
import com.dbn.object.type.DBJavaClassType;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.util.Java.getQualifiedClassName;
import static com.dbn.common.util.Strings.isEmpty;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.factory.model.DBObjectAttributeType.JAVA_CLASS_NAME;
import static com.dbn.object.factory.model.DBObjectAttributeType.JAVA_CLASS_TYPE;
import static com.dbn.object.factory.model.DBObjectAttributeType.JAVA_PACKAGE_NAME;
import static com.dbn.object.type.DBJavaClassType.CLASS;
import static com.dbn.object.type.DBJavaClassType.EXCEPTION;
import static com.dbn.object.type.DBObjectType.JAVA_CLASS;

public class DBJavaClassFactoryAdapter implements ObjectFactoryAdapter {

    @Override
    public DBObjectType getObjectType() {
        return JAVA_CLASS;
    }

    public DBObjectSpec createInput(DBSchema schema) {
        DBObjectSpec input = new DBObjectSpec(schema, JAVA_CLASS);
        input.setAttributeValue(JAVA_CLASS_TYPE, CLASS);
        return input;
    }

    public DBJavaClassFactoryInputForm createInputForm(DBNComponent parent, DBObjectSpec input) {
        return new DBJavaClassFactoryInputForm(parent, input);
    }

    @Override
    public void validateInput(DBObjectSpec input, List<String> errors) {
        // TODO
    }

    @Override
    public void createObject(DBObjectSpec input) throws SQLException {
        String className = JAVA_CLASS_NAME.of(input);
        String packageName = JAVA_PACKAGE_NAME.of(input);

        DBJavaClassType javaClassType = getClassType(input);
        String classType = getTypeIdentifier(javaClassType);
        String extendsSuffix = getExtendsSuffix(javaClassType);
        DBSchema schema = input.getSchema();

        StringBuilder javaCode = new StringBuilder();
        if(isNotEmpty(packageName)) {
            javaCode.append("package ").append(packageName).append(";").append("\n");
        }

        javaCode.append("public ").append(classType).append(" ").append(className).append(extendsSuffix)
                .append("{")
                .append("\n")
                .append("}");

        String objectName = getDatabaseObjectName(input);
        ConnectionId connectionId = schema.getConnectionId();
        SchemaId schemaId = schema.getSchemaId();

        DatabaseInterfaceInvoker.execute(HIGHEST,
                txt("prc.object.title.CreatingObject", input.getObjectType().getTitleCasedName()),
                txt("prc.object.text.CreatingObjectDescription", getObjectDescription(input)),
                schema.getProject(),
                connectionId,
                conn -> {
                    ConnectionHandler connection = schema.getConnection();
                    DatabaseIdentifierCache identifierCache = connection.getIdentifierCache();
                    String quotedObjectName = identifierCache.getQuotedIdentifier(objectName);

                    DatabaseJavaInterface javaInterface = connection.getJavaInterface();
                    javaInterface.createJavaSource(schema.getName(true), quotedObjectName, javaCode.toString().getBytes(), conn);
                });

        ObjectChangeEvent.notify(CREATE, JAVA_CLASS, connectionId, schemaId);

        DBJavaClass javaClass = schema.getChildObject(JAVA_CLASS, objectName, false);
        if (javaClass == null) return;

        Project project = input.getProject();
        DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(project);
        editorManager.connectAndOpenEditor(javaClass, null, false, true);
    }


    private static DBJavaClassType getClassType(DBObjectSpec input) {
        DBJavaClassType classType = JAVA_CLASS_TYPE.of(input);
        return classType == null ? CLASS : classType;
    }

    private static String getExtendsSuffix(DBJavaClassType classType) {
        return classType == EXCEPTION ? " extends Exception " : "";
    }

    private static String getDatabaseObjectName(DBObjectSpec input) {
        String packageName = JAVA_PACKAGE_NAME.of(input);
        String className = JAVA_CLASS_NAME.of(input);
        if (isEmpty(packageName)) return className;

        return packageName.replace(".", "/") + "/" + className;
    }

    private static String getObjectDescription(DBObjectSpec input) {
        String objectName = "\"" + getQualifiedClassName(JAVA_PACKAGE_NAME.of(input), JAVA_CLASS_NAME.of(input)) + "\"";
        return switch (getClassType(input)) {
            case INTERFACE -> "java interface " + objectName;
            case ANNOTATION -> "java annotation " + objectName;
            case EXCEPTION -> "java exception " + objectName;
//            case RECORD -> "java record " + objectName;
            case ENUM -> "java enumeration " + objectName;
            default -> "java class " + objectName;
        };
    }

    private static String getTypeIdentifier(DBJavaClassType classType) {
        return switch (classType) {
            case INTERFACE -> "interface";
            case ANNOTATION -> "@interface";
//            case RECORD -> "record";
            case ENUM -> "enum";
            default -> "class";
        };
    }
}
