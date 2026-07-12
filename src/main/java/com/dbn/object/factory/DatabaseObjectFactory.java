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

package com.dbn.object.factory;

import com.dbn.common.component.Components;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.routine.Consumer;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Safe;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseJavaInterface;
import com.dbn.editor.DBContentType;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.ui.common.DBObjectFactoryInputDialog;
import com.dbn.object.management.ObjectManagementService;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DatabaseFileManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Messages.OPTIONS_YES_CANCEL;
import static com.dbn.common.util.Messages.OPTIONS_YES_NO;
import static com.dbn.common.util.Messages.showErrorDialog;
import static com.dbn.common.util.Messages.showQuestionDialog;
import static com.dbn.common.util.Messages.whenOk;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.DELETE;
import static com.dbn.object.type.DBObjectType.CREDENTIAL;
import static com.dbn.object.type.DBObjectType.JAVA_CLASS;

public class DatabaseObjectFactory extends ProjectComponentBase {

    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseObjectFactory";

    private DatabaseObjectFactory(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DatabaseObjectFactory getInstance(@NotNull Project project) {
        return Components.projectService(project, DatabaseObjectFactory.class);
    }

    public void openFactoryInputDialog(
            @NotNull DBSchema schema,
            @NotNull DBObjectType objectType) {
        openFactoryInputDialog(schema, objectType, null);
    }

    public void openFactoryInputDialog(
            @NotNull DBSchema schema,
            @NotNull DBObjectType objectType,
            @Nullable DBObjectSpec initialInput) {
        openFactoryInputDialog(schema, objectType, initialInput, (d, c) -> {});
    }

    public void openFactoryInputDialog(
            @NotNull DBSchema schema,
            @NotNull DBObjectType objectType,
            @Nullable DBObjectSpec initialInput,
            @Nullable Consumer<String> objectNameConsumer) {
        openFactoryInputDialog(schema, objectType, initialInput,
                (d, c) -> when(
                        c == DialogWrapper.OK_EXIT_CODE,
                        () -> Safe.run(objectNameConsumer, nc -> nc.accept(d.getObjectName().toUpperCase()))));
    }

    private void openFactoryInputDialog(
            @NotNull DBSchema schema,
            @NotNull DBObjectType objectType,
            @Nullable DBObjectSpec initialInput,
            @Nullable Dialogs.DialogCallback<DBObjectFactoryInputDialog> callback) {
        Project project = getProject();


        if (ObjectFactoryAdapters.isSupported(objectType)) {
            if (isOwnerRestricted(objectType) && !schema.isUserSchema()) {
                String objectTypeName = objectType.getDisplayName();
                ConnectionHandler connection = schema.getConnection();
                DBSchema userSchema = connection.getUserSchema();

                showQuestionDialog(project,
                        txt("msg.objects.title.OwnerRestriction"),
                        txt("msg.objects.question.OwnerRestriction", objectTypeName),
                        OPTIONS_YES_CANCEL, 0,
                        whenOk(() -> openFactoryInputDialog(
                                userSchema,
                                objectType,
                                initialInput,
                                callback)));
                return;
            }


            Dialogs.show(() -> new DBObjectFactoryInputDialog(project, schema, objectType, initialInput), callback);
        } else {
            showErrorDialog(project,
                    txt("msg.objects.title.OperationNotSupported"),
                    txt("msg.objects.error.ObjectCreationNotSupported", objectType.getListDisplayName()));
        }
    }

    private boolean isOwnerRestricted(DBObjectType objectType) {
        if (objectType == CREDENTIAL) return true;
        //...

        return false;
    }

    public void createObject(DBObjectSpec input) throws SQLException {
        Project project = getProject();

        DBObjectType objectType = input.getObjectType();
        ObjectFactoryAdapter factoryAdapter = ObjectFactoryAdapters.get(objectType);

        List<String> errors = new ArrayList<>();
        factoryAdapter.validateInput(input, errors);

        if (errors.isEmpty()) {
            factoryAdapter.createObject(input);
        } else {
            String objectTypeName = objectType.getDisplayName();
            String objectErrors = errors.stream().map(error -> " - " + error + "\n").collect(Collectors.joining());
            showErrorDialog(project, txt("msg.objects.error.ObjectCreationError", objectTypeName, objectErrors));
        }

    }

    public void dropObject(DBSchemaObject object) {
        Project project = getProject();
        showQuestionDialog(
                project,
                txt("msg.objects.title.DropObject"),
                txt("msg.objects.question.DropObject", object.getQualifiedNameWithType()),
                OPTIONS_YES_NO, 0,
                whenOk(() ->
                        ConnectionAction.invoke(txt("msg.objects.title.DroppingObject"), false, object, action -> {
                            DatabaseFileManager databaseFileManager = DatabaseFileManager.getInstance(project);
                            databaseFileManager.closeFile(object);

                            ObjectManagementService objectManagementService = ObjectManagementService.getInstance(project);
                            if (objectManagementService.supports(object)) {
                                objectManagementService.deleteObject(object, null);
                                return;
                            }

                            // TODO old implementation (implement appropriate ObjectManagementServices and cleanup)
                            Progress.prompt(project, object, false,
                                    txt("prc.objects.title.DroppingObject"),
                                     txt("prc.objects.text.DroppingObject", object.getQualifiedNameWithType()),
                                    progress -> doDropObject(object));
                        })));

    }

    @Deprecated // TODO old implementation (implement appropriate ObjectManagementServices and cleanup)
    private void doDropObject(DBSchemaObject object) {
        Project project = getProject();
        try {
            ConnectionId connectionId = object.getConnectionId();
            SchemaId schemaId = object.getSchemaId();
            DatabaseInterfaceInvoker.execute(HIGHEST,
                    txt("prc.objects.title.DroppingObject"),
                    txt("prc.objects.text.DroppingObject", object.getQualifiedNameWithType()),
                    project,
                    connectionId,
                    conn -> {
                        DBObjectType objectType = object.getObjectType();
                        DBContentType contentType = object.getContentType();

                        String schemaName = object.getSchemaName(true);
                        String objectName = object.getName(true);

                        String objectTypeName = object.getTypeName();
                        DatabaseDataDefinitionInterface dataDefinition = object.getDataDefinitionInterface();
                        if (contentType == DBContentType.CODE_SPEC_AND_BODY) {
                            DBObjectStatusHolder objectStatus = object.getStatus();
                            if (objectStatus.is(DBContentType.CODE_BODY, DBObjectStatus.PRESENT)) {
                                dataDefinition.dropObjectBody(objectTypeName, schemaName, objectName, conn);
                            }

                            if (objectStatus.is(DBContentType.CODE_SPEC, DBObjectStatus.PRESENT)) {
                                dataDefinition.dropObject(objectTypeName, schemaName, objectName, conn);
                            }
                        } else {
                            if(objectType == JAVA_CLASS) {
                                DatabaseJavaInterface javaInterface = object.getJavaInterface();
                                javaInterface.dropJavaClass(schemaName, objectName, conn);
                            } else {
                                dataDefinition.dropObject(objectTypeName, schemaName, objectName, conn);

                            }
                        }

                        ObjectChangeEvent.notify(DELETE, object);
                    });
        } catch (SQLException e) {
            conditionallyLog(e);
            String message = txt("msg.objects.error.CouldNotDropObject", object.getQualifiedNameWithType());
            showErrorDialog(project, message, e);
        }
    }
}
