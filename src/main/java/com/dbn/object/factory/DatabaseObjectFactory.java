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
import com.dbn.common.event.ProjectEvents;
import com.dbn.common.thread.Callback;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.editor.DBContentType;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.object.DBMethod;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.factory.ui.common.ObjectFactoryInputDialog;
import com.dbn.object.management.ObjectManagementService;
import com.dbn.object.type.DBObjectType;
import com.dbn.vfs.DatabaseFileManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

public class DatabaseObjectFactory extends ProjectComponentBase {

    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseObjectFactory";

    private DatabaseObjectFactory(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DatabaseObjectFactory getInstance(@NotNull Project project) {
        return Components.projectService(project, DatabaseObjectFactory.class);
    }

    private void notifyFactoryEvent(ObjectFactoryEvent event) {
        DBSchemaObject object = event.getObject();
        int eventType = event.getEventType();
        Project project = getProject();
        if (eventType == ObjectFactoryEvent.EVENT_TYPE_CREATE) {
            ProjectEvents.notify(project,
                    ObjectFactoryListener.TOPIC,
                    (listener) -> listener.objectCreated(object));

        } else if (eventType == ObjectFactoryEvent.EVENT_TYPE_DROP) {
            ProjectEvents.notify(project,
                    ObjectFactoryListener.TOPIC,
                    (listener) -> listener.objectDropped(object));
        }
    }


    public void openFactoryInputDialog(DBSchema schema, DBObjectType objectType) {
        Project project = getProject();
        if (objectType.isOneOf(DBObjectType.FUNCTION, DBObjectType.PROCEDURE)) {
            Dialogs.show(() -> new ObjectFactoryInputDialog(project, schema, objectType));
        } else {
            Messages.showErrorDialog(project,
                    txt("msg.objects.title.OperationNotSupported"),
                    txt("msg.objects.error.ObjectCreationNotSupported", objectType.getListName()));
        }
    }

    public void createObject(ObjectFactoryInput factoryInput, Callback callback) {
        Project project = getProject();
        List<String> errors = new ArrayList<>();
        factoryInput.validate(errors);
        if (!errors.isEmpty()) {
            String objectType = factoryInput.getObjectType().getName();
            String objectErrors = errors.stream().map(error -> " - " + error + "\n").collect(Collectors.joining());
            Messages.showErrorDialog(project, txt("msg.objects.error.ObjectCreationError", objectType, objectErrors));
            return;
        }

        if (factoryInput instanceof MethodFactoryInput) {
            MethodFactoryInput methodFactoryInput = (MethodFactoryInput) factoryInput;
            createMethod(methodFactoryInput, callback);
        }
        // TODO other factory inputs
    }

    private void createMethod(MethodFactoryInput factoryInput, Callback callback) {
        callback.background(() -> {
            DBObjectType objectType = factoryInput.isFunction() ? DBObjectType.FUNCTION : DBObjectType.PROCEDURE;
            String objectTypeName = objectType.getName();
            String objectName = factoryInput.getObjectName();
            DBSchema schema = factoryInput.getSchema();

            DatabaseInterfaceInvoker.execute(HIGHEST,
                    "Creating " + objectTypeName,
                    "Creating " + objectTypeName + " " + objectName,
                    schema.getProject(),
                    schema.getConnectionId(),
                    schema.getSchemaId(),
                    conn -> {
                        DatabaseDataDefinitionInterface dataDefinition = schema.getDataDefinitionInterface();
                        dataDefinition.createMethod(factoryInput, conn);
                    });

            nn(schema.getChildObjectList(objectType)).reload();

            DBMethod method = schema.getChildObject(objectType, objectName, false);
            nn(method.getChildObjectList(DBObjectType.ARGUMENT)).reload();

            DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(getProject());
            editorManager.connectAndOpenEditor(method, null, false, true);
            notifyFactoryEvent(new ObjectFactoryEvent(method, ObjectFactoryEvent.EVENT_TYPE_CREATE));
        });
    }

    public void dropObject(DBSchemaObject object) {
        Messages.showQuestionDialog(
                getProject(),
                txt("msg.objects.title.DropObject"),
                txt("msg.objects.question.DropObject", object.getQualifiedNameWithType()),
                Messages.OPTIONS_YES_NO, 0,
                option -> when(option == 0, () ->
                        ConnectionAction.invoke(txt("msg.objects.title.DroppingObject"), false, object, action -> {
                            Project project = getProject();
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
        try {
            DatabaseInterfaceInvoker.execute(HIGHEST,
                    txt("prc.objects.title.DroppingObject"),
                    txt("prc.objects.text.DroppingObject", object.getQualifiedNameWithType()),
                    object.getProject(),
                    object.getConnectionId(),
                    conn -> {
                        DBContentType contentType = object.getContentType();

                        String schemaName = object.getSchemaName(true);
                        String objectName = object.getName(true);

                        String objectTypeName = object.getTypeName();
                        DatabaseDataDefinitionInterface dataDefinition = object.getDataDefinitionInterface();
                        DBObjectList<?> objectList = (DBObjectList<?>) object.getParent();
                        if (contentType == DBContentType.CODE_SPEC_AND_BODY) {
                            DBObjectStatusHolder objectStatus = object.getStatus();
                            if (objectStatus.is(DBContentType.CODE_BODY, DBObjectStatus.PRESENT)) {
                                dataDefinition.dropObjectBody(objectTypeName, schemaName, objectName, conn);
                            }

                            if (objectStatus.is(DBContentType.CODE_SPEC, DBObjectStatus.PRESENT)) {
                                dataDefinition.dropObject(objectTypeName, schemaName, objectName, conn);
                            }
                        } else if(object.getObjectType() == DBObjectType.JAVA_CLASS) {
                            dataDefinition.dropJavaClass(schemaName, objectName, conn);
                        } else {
                            dataDefinition.dropObject(objectTypeName, schemaName, objectName, conn);
                        }

                        objectList.reload();
                        notifyFactoryEvent(new ObjectFactoryEvent(object, ObjectFactoryEvent.EVENT_TYPE_DROP));
                    });
        } catch (SQLException e) {
            conditionallyLog(e);
            String message = "Could not drop " + object.getQualifiedNameWithType() + ".";
            Project project = getProject();
            Messages.showErrorDialog(project, message, e);
        }
    }
}