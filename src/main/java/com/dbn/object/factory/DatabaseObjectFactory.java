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

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.dispose.Failsafe.nn;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

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
            Messages.showErrorDialog(project, "Operation not supported", "Creation of " + objectType.getListName() + " is not supported yet.");
        }
    }

    public void createObject(ObjectFactoryInput factoryInput, Callback callback) {
        Project project = getProject();
        List<String> errors = new ArrayList<>();
        factoryInput.validate(errors);
        if (errors.size() > 0) {
            StringBuilder buffer = new StringBuilder("Could not create " + factoryInput.getObjectType().getName() + ". Please correct following errors: \n");
            for (String error : errors) {
                buffer.append(" - ").append(error).append("\n");
            }
            Messages.showErrorDialog(project, buffer.toString());
        }

        if (factoryInput instanceof MethodFactoryInput) {
            MethodFactoryInput methodFactoryInput = (MethodFactoryInput) factoryInput;
            createMethod(methodFactoryInput, callback);
        }
        // TODO other factory inputs
    }

    private void createMethod(MethodFactoryInput factoryInput, Callback callback) {
        callback.background(getProject(), () -> {
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
                "Drop object",
                "Are you sure you want to drop the " + object.getQualifiedNameWithType() + "?",
                Messages.OPTIONS_YES_NO, 0,
                option -> when(option == 0, () ->
                        ConnectionAction.invoke("dropping the object", false, object, action -> {
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
                                    "Dropping object",
                                    "Dropping " + object.getQualifiedNameWithType(),
                                    progress -> doDropObject(object));
                        })));

    }

    @Deprecated // TODO old implementation (implement appropriate ObjectManagementServices and cleanup)
    private void doDropObject(DBSchemaObject object) {
        try {
            DatabaseInterfaceInvoker.execute(HIGHEST,
                    "Dropping database object",
                    "Dropping " + object.getQualifiedNameWithType(),
                    object.getProject(),
                    object.getConnectionId(),
                    conn -> {
                        DBContentType contentType = object.getContentType();

                        String objectName = object.getQualifiedName();
                        String objectTypeName = object.getTypeName();
                        DatabaseDataDefinitionInterface dataDefinition = object.getDataDefinitionInterface();
                        DBObjectList<?> objectList = (DBObjectList<?>) object.getParent();
                        if (contentType == DBContentType.CODE_SPEC_AND_BODY) {
                            DBObjectStatusHolder objectStatus = object.getStatus();
                            if (objectStatus.is(DBContentType.CODE_BODY, DBObjectStatus.PRESENT)) {
                                dataDefinition.dropObjectBody(objectTypeName, objectName, conn);
                            }

                            if (objectStatus.is(DBContentType.CODE_SPEC, DBObjectStatus.PRESENT)) {
                                dataDefinition.dropObject(objectTypeName, objectName, conn);
                            }

                        } else {
                            dataDefinition.dropObject(objectTypeName, objectName, conn);
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