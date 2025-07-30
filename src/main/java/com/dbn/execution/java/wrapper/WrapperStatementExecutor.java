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
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.execution.java.wrapper.model.MethodWrapper;
import com.dbn.execution.java.wrapper.naming.CustomNamingProvider;
import com.dbn.execution.java.wrapper.ui.WrapperCustomNamingProviderDialog;
import com.dbn.execution.java.wrapper.ui.WrapperResultDialog;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBMethod;
import com.dbn.object.common.DBObject;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import lombok.experimental.UtilityClass;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.type.DBObjectType.TYPE;

@UtilityClass
public class WrapperStatementExecutor {
    public static void createExecutionWrappers(WrapperModel model) throws SQLException {
        if (model.getInput().isClassLevel()) {
            createClassExecutionWrappers(model);
        } else {
            createMethodExecutionWrappers(model);
        }
    }

    private static void createMethodExecutionWrappers(WrapperModel model) throws SQLException {
        Project project = model.getProject();
        DBJavaMethod javaMethod = model.getSourceObject();
        ConnectionId connectionId = javaMethod.getConnectionId();

        boolean isLongNameWrapper = isLongNameWrapper(model, false);
        if (!isLongNameWrapper) {
            createWrapper(model, false);
            return;
        }

        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Checking Identifier length limit for wrappers",
                "Checking Identifier length limit for wrappers for \"" + javaMethod.getPresentableText() + "\"",
                project,
                connectionId, c -> {
                    DatabaseMetadataInterface databaseMetadataInterface = model.getMetadataInterface();
                    boolean longIdentifierSupported = databaseMetadataInterface.checkLongIdentifier(c).isValue();

                    if (longIdentifierSupported) {
                        createWrapper(model, false);
                        return;
                    }

                    Dialogs.show(() -> new WrapperCustomNamingProviderDialog(model.getProject(), model, false), (dialog, exitCode) -> {
                        if (exitCode != DialogWrapper.OK_EXIT_CODE) return;

                        CustomNamingProvider namingProvider = new CustomNamingProvider();
                        namingProvider.setJavaWrapperName(dialog.getJavaWrapperName());
                        namingProvider.setSqlWrapperName(dialog.getSqlWrapperName());
                        namingProvider.setSqlTypesMap(dialog.getSqlTypeNames());

                        WrapperModel newModel = WrapperModelBuilder.getInstance().buildModel(model.getInput(), namingProvider);
                        try {
                            createWrapper(newModel, false);
                        } catch (SQLException e) {
                            Messages.showErrorDialog(project,"Error creating execution wrappers for java methods \nCause: " + e.getMessage());
                            conditionallyLog(e);
                        }
                    });
                });
    }

    private  static void createClassExecutionWrappers(WrapperModel model) throws SQLException {
        WrapperModelInput input = model.getInput();

        List<DBJavaMethod> methods = input.getJavaMethods();
        if (methods.isEmpty()) return;

        DBJavaClass javaClass = model.getSourceObject();

        Project project = javaClass.getProject();
        ConnectionId connectionId = javaClass.getConnectionId();

        boolean isLongNameWrapper = isLongNameWrapper(model, true);
        if (!isLongNameWrapper) {
            createWrapper(model, true);
            return;
        }

        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Checking Identifier length limit for wrappers",
                "Checking Identifier length limit for wrappers for \"" + javaClass.getCanonicalName() + "\"",
                project,
                connectionId, c -> {
                    DatabaseMetadataInterface databaseMetadataInterface = model.getMetadataInterface();
                    boolean longIdentifierSupported = databaseMetadataInterface.checkLongIdentifier(c).isValue();

                    if (longIdentifierSupported) {
                        createWrapper(model, true);
                        return;
                    }

                    Dialogs.show(() -> new WrapperCustomNamingProviderDialog(model.getProject(), model, true), (dialog, exitCode) -> {
                        if (exitCode != DialogWrapper.OK_EXIT_CODE) return;

                        CustomNamingProvider namingProvider = new CustomNamingProvider();
                        namingProvider.setJavaWrapperName(dialog.getJavaWrapperName());
                        namingProvider.setSqlWrapperName(dialog.getSqlWrapperName());
                        namingProvider.setSqlTypesMap(dialog.getSqlTypeNames());
                        namingProvider.setSqlPackageMethodMap(dialog.getPackageMethodNames());

                        WrapperModel newModel = WrapperModelBuilder.getInstance().buildModel(model.getInput(), namingProvider);
                        try {
                            createWrapper(newModel, true);
                        } catch (SQLException e) {
                            Messages.showErrorDialog(project,"Error creating execution wrappers for java methods \nCause: " + e.getMessage());
                            conditionallyLog(e);
                        }
                    });
                });
    }

    private static void createWrapper(WrapperModel model, boolean classLevel) throws SQLException {
        WrapperModelInput input = model.getInput();

        Project project = model.getProject();
        WrapperStatementBuilder statementBuilder = new WrapperStatementBuilder(project);
        String creationStatement = statementBuilder.buildWrapperCreationStatement(model);

        ConnectionId connectionId = model.getSourceObject().getConnectionId();

        String presentableText;
        if(classLevel){
            presentableText = ((DBJavaClass) model.getSourceObject()).getCanonicalName();
        } else {
            presentableText = model.getSourceObject().getPresentableText();
        }

        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Creating execution wrappers",
                "Creating java execution wrappers for method \"" + presentableText + "\"",
                project,
                connectionId, c -> {
                    c.executeStatement(creationStatement);
                    if (input.isCompileInDebugMode()) {
                        compileObjectInDebugMode(c, model);
                    }

                    if (input.isUseFriendlyNames()) {
                        if(classLevel) {
                            DBJavaClass javaClass = model.getSourceObject();
                            notifyObjectChanges(javaClass, DBObjectType.JAVA_CLASS, CREATE);
                            notifyObjectChanges(javaClass, TYPE, CREATE);
                            notifyObjectChanges(javaClass, DBObjectType.PACKAGE, CREATE);
                        } else {
                            DBJavaMethod javaMethod = model.getSourceObject();
                            notifyObjectChanges(javaMethod, DBObjectType.JAVA_CLASS, CREATE);
                            notifyObjectChanges(javaMethod, TYPE, CREATE);
                            notifyObjectChanges(javaMethod, DBObjectType.FUNCTION, CREATE);
                            notifyObjectChanges(javaMethod, DBObjectType.PROCEDURE, CREATE);
                        }
                        showWrapperResult(model);
                    }
                });
    }

    public static void discardExecutionWrappers(WrapperModel model) throws SQLException {
        if (model == null) return;

        // temporary wrappers - source object is expected to be a method
        DBJavaMethod method = model.getSourceObject();

        Project project = method.getProject();
        WrapperStatementBuilder statementBuilder = new WrapperStatementBuilder(project);
        String removalStatement = statementBuilder.buildWrapperRemovalStatement(model);

        ConnectionId connectionId = method.getConnectionId();
        DatabaseInterfaceInvoker.execute(Priority.HIGH,
                "Removing execution wrappers",
                "Removing java execution wrappers for " + method.getPresentableText(),
                project,
                connectionId, c -> c.executeStatement(removalStatement));
    }

    public static void notifyObjectChanges(DBObject sourceObject, DBObjectType objectType, ObjectChangeAction action) {
        Project project = sourceObject.getProject();
        ConnectionId connectionId = sourceObject.getConnectionId();
        SchemaId schemaId = sourceObject.getSchemaId();
        ProjectEvents.notify(project, ObjectChangeListener.TOPIC, l -> l.objectsChanged(connectionId, schemaId, objectType, action));
    }

    private static void compileObjectInDebugMode(DBNConnection connection, WrapperModel model) throws SQLException {
        String schemaName = model.getSchemaName();
        DatabaseDataDefinitionInterface dataDefinitionInterface = model.getDataDefinitionInterface();

        // compile wrapper types
        for (String typeName : model.getSqlTypeNames()) {
            dataDefinitionInterface.compileObject(schemaName, typeName, TYPE.getName(), true, connection);
        }

        // compile wrapper method
        DBObjectRef<DBMethod> method = model.getSqlWrapperMethod();
        String methodName = method.getObjectName();
        String methodType = method.getObjectTypeName();
        dataDefinitionInterface.compileObject(schemaName, methodName, methodType, true, connection);

        // compile java wrapper
        dataDefinitionInterface.compileJavaClass(schemaName, model.getJavaWrapperName(), connection);
    }

    public void showWrapperResult(WrapperModel model) {
        Dialogs.show(() -> new WrapperResultDialog(model.getProject(), model));
    }

    private static boolean isLongNameWrapper(WrapperModel model, boolean isClassLevel) {
        Set<String> sqlTypeNames = model.getSqlTypeNames();
        for (String sqlType : sqlTypeNames) {
            if (sqlType.length() > 30) {
                return true;
            }
        }
        if (model.getSqlWrapperName().length() > 30) return true;
        if (model.getJavaWrapperName().length() > 30) return true;
        if (isClassLevel){
            for(MethodWrapper methodWrapper: model.getMethods()){
                if(methodWrapper.getSqlMethodName().length() > 30) return true;
            }
        }

        return false;
    }
}
