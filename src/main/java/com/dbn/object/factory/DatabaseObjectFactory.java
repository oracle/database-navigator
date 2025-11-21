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
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.thread.Progress;
import com.dbn.common.util.Dialogs;
import com.dbn.common.util.Messages;
import com.dbn.connection.ConnectionAction;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.connection.security.DatabaseIdentifierCache;
import com.dbn.database.interfaces.DatabaseDataDefinitionInterface;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.editor.DBContentType;
import com.dbn.editor.DatabaseFileEditorManager;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBMethod;
import com.dbn.object.DBSchema;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.common.status.DBObjectStatus;
import com.dbn.object.common.status.DBObjectStatusHolder;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.ui.common.ObjectFactoryInputDialog;
import com.dbn.object.management.ObjectManagementService;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.common.ModelSourceType;
import com.dbn.vfs.DatabaseFileManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.dbn.common.Priority.HIGHEST;
import static com.dbn.common.Priority.MEDIUM;
import static com.dbn.common.util.Conditional.when;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.event.ObjectChangeAction.DELETE;
import static com.dbn.object.type.DBObjectType.AI_MODEL;
import static com.dbn.object.type.DBObjectType.FUNCTION;
import static com.dbn.object.type.DBObjectType.JAVA_CLASS;
import static com.dbn.object.type.DBObjectType.PROCEDURE;

public class DatabaseObjectFactory extends ProjectComponentBase {

    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseObjectFactory";

    private DatabaseObjectFactory(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DatabaseObjectFactory getInstance(@NotNull Project project) {
        return Components.projectService(project, DatabaseObjectFactory.class);
    }

    public void openFactoryInputDialog(DBSchema schema, DBObjectType objectType) {
        Project project = getProject();
        if (objectType.isOneOf(FUNCTION, PROCEDURE, JAVA_CLASS, AI_MODEL)) {
            Dialogs.show(() -> new ObjectFactoryInputDialog(project, schema, objectType));
        } else {
            Messages.showErrorDialog(project,
                    txt("msg.objects.title.OperationNotSupported"),
                    txt("msg.objects.error.ObjectCreationNotSupported", objectType.getListName()));
        }
    }

    public void createObject(ObjectFactoryInput factoryInput) throws SQLException {
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
            createMethod(methodFactoryInput);
            return;
        }

        if (factoryInput instanceof JavaFactoryInput) {
            JavaFactoryInput javaFactoryInput = (JavaFactoryInput) factoryInput;
            createJavaObject(javaFactoryInput);
            return;
        }

        if (factoryInput instanceof ModelFactoryInput) {
            ModelFactoryInput modelFactoryInput = (ModelFactoryInput) factoryInput;
            createModel(modelFactoryInput);
            return ;
        }
        // TODO other factory inputs

    }

    private void createModel(ModelFactoryInput input) throws SQLException {
        ModelSourceType modelSourceType = input.getSourceType();
        DBSchema schema = input.getSchema();

        ConnectionId connectionId = schema.getConnectionId();
        SchemaId schemaId = schema.getSchemaId();

        ProgressIndicator progress = ProgressMonitor.ensureProgressIndicator();

        DatabaseInterfaceInvoker.execute(MEDIUM,
                "Creating " + input.getObjectType().getTitleCasedName(),
                "Creating " + input.getObjectDescription(),
                schema.getProject(),
                connectionId,
                schemaId,
                conn -> {
                    DatabaseVectorInterface dataDefinition = schema.getVectorInterface();
                    if (modelSourceType == ModelSourceType.OBJECT_STORAGE) {
                        dataDefinition.loadOnnxModelFromOci(input, conn);

                    } else if (modelSourceType == ModelSourceType.MODEL_FILE){
                        Blob modelBlob = uploadOnnxModel(conn,input, progress);
                        dataDefinition.loadOnnxModelThroughJdbc(input.getModelName(),modelBlob, conn);

                    } else {
                        throw new IllegalArgumentException("Unsupported model source type: " + modelSourceType);
                    }
                });

        ObjectChangeEvent.notify(CREATE, AI_MODEL, connectionId, schemaId);
    }

    private Blob uploadOnnxModel(
            DBNConnection conn,
            ModelFactoryInput input,
            ProgressIndicator progress
    ) throws SQLException {
        File modelFile = new File(input.getSourceLocation());
        long fileSize      = modelFile.length();
        double totalMB     = fileSize / (1024.0 * 1024.0);

        // Tell the ProgressIndicator what we're doing
        progress.setIndeterminate(false);
        progress.setText("Uploading ONNX model \"" + modelFile.getName() + "\" as " + input.getSchema().getName(true) + ".\"" + input.getModelName() + "\"");
        progress.setFraction(0.0);

        Blob modelBlob = conn.createBlob();

        try (RandomAccessFile randomFile = new RandomAccessFile(modelFile, "r");
             FileChannel     fileChannel = randomFile.getChannel();
             OutputStream    dbOutput    = modelBlob.setBinaryStream(1)) {

            MappedByteBuffer mappedFile  = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY, 0, fileSize
            );
            byte[] chunk = new byte[1024 * 1024];  // 1 MB buffer
            long bytesUploaded = 0;

            while (mappedFile.hasRemaining()) {
                // allow user to cancel
                progress.checkCanceled();

                int toRead = (int)Math.min(chunk.length, mappedFile.remaining());
                mappedFile.get(chunk, 0, toRead);
                dbOutput.write(chunk, 0, toRead);

                bytesUploaded += toRead;
                double fraction = bytesUploaded / (double)fileSize;

                // update the progress bar
                progress.setFraction(fraction);
                progress.setText2(String.format(
                        "Uploaded %.1f MB of %.1f MB",
                        bytesUploaded / (1024.0 * 1024.0),
                        totalMB
                ));
            }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }

      // final update (100%)
        progress.setFraction(1.0);
        progress.setText("Upload complete");



        return modelBlob;
    }



    private void createMethod(MethodFactoryInput input) throws SQLException {
        DBObjectType objectType = input.isFunction() ? FUNCTION : PROCEDURE;
        String objectName = input.getObjectName();
        DBSchema schema = input.getSchema();

        ConnectionId connectionId = schema.getConnectionId();
        SchemaId schemaId = schema.getSchemaId();

        DatabaseInterfaceInvoker.execute(HIGHEST,
                "Creating " + input.getObjectType().getTitleCasedName(),
                "Creating " + input.getObjectDescription(),
                schema.getProject(),
                connectionId,
                schemaId,
                conn -> {
                    DatabaseDataDefinitionInterface dataDefinition = schema.getDataDefinitionInterface();
                    dataDefinition.createMethod(input, conn);
                });

        ObjectChangeEvent.notify(CREATE, objectType, connectionId, schemaId);

        DBMethod method = schema.getChildObject(objectType, objectName, false);
        if (method == null) return;

        DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(getProject());
        editorManager.connectAndOpenEditor(method, null, false, true);
    }

    private void createJavaObject(JavaFactoryInput input) throws SQLException {
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

        DatabaseFileEditorManager editorManager = DatabaseFileEditorManager.getInstance(getProject());
        editorManager.connectAndOpenEditor(javaClass, null, false, true);
    }

    public void dropObject(DBSchemaObject object) {
        Project project = getProject();
        Messages.showQuestionDialog(
                project,
                txt("msg.objects.title.DropObject"),
                txt("msg.objects.question.DropObject", object.getQualifiedNameWithType()),
                Messages.OPTIONS_YES_NO, 0,
                option -> when(option == 0, () ->
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
                                dataDefinition.dropJavaClass(schemaName, objectName, conn);
                            } else {
                                dataDefinition.dropObject(objectTypeName, schemaName, objectName, conn);

                            }
                        }

                        ObjectChangeEvent.notify(DELETE, object);
                    });
        } catch (SQLException e) {
            conditionallyLog(e);
            String message = "Could not drop " + object.getQualifiedNameWithType() + ".";
            Messages.showErrorDialog(project, message, e);
        }
    }
}