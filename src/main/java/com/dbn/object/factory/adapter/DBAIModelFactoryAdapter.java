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

import com.dbn.common.exception.Exceptions;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.interfaces.DatabaseInterfaceInvoker;
import com.dbn.database.interfaces.DatabaseVectorInterface;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.ObjectFactoryAdapter;
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.ui.DBAIModelFactoryInputForm;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBAIModelSourceType;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.progress.ProgressIndicator;

import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.Priority.MEDIUM;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.factory.model.DBObjectAttributeType.AI_MODEL_CREDENTIAL;
import static com.dbn.object.factory.model.DBObjectAttributeType.AI_MODEL_SOURCE_LOCATION;
import static com.dbn.object.factory.model.DBObjectAttributeType.AI_MODEL_SOURCE_TYPE;
import static com.dbn.object.type.DBAIModelSourceType.MODEL_FILE;
import static com.dbn.object.type.DBObjectType.AI_MODEL;

public class DBAIModelFactoryAdapter implements ObjectFactoryAdapter {

    @Override
    public DBObjectType getObjectType() {
        return AI_MODEL;
    }

    @Override
    public DBObjectSpec createInput(DBSchema schema) {
        DBObjectSpec input = new DBObjectSpec(schema, AI_MODEL);
        input.setAttributeValue(AI_MODEL_SOURCE_TYPE, MODEL_FILE);
        return input;
    }

    public DBAIModelFactoryInputForm createInputForm(DBNComponent parent, DBObjectSpec input) {
        return new DBAIModelFactoryInputForm(parent, input);
    }

    @Override
    public void validateInput(DBObjectSpec input, List<String> errors) {
        // TODO
    }

    @Override
    public void createObject(DBObjectSpec input) throws SQLException {
        DBAIModelSourceType modelSourceType = AI_MODEL_SOURCE_TYPE.of(input);

        DBObjectType objectType = input.getObjectType();
        DBSchema schema = input.getSchema();
        ConnectionId connectionId = schema.getConnectionId();
        SchemaId schemaId = schema.getSchemaId();

        ProgressIndicator progress = ProgressMonitor.ensureProgressIndicator();

        DatabaseInterfaceInvoker.execute(MEDIUM,
                txt("prc.object.title.CreatingObject", objectType.getTitleCasedDisplayName()),
                txt("prc.object.text.CreatingObjectDescription", input.getObjectDescription()),
                schema.getProject(),
                connectionId,
                conn -> {
                    DatabaseVectorInterface dataDefinition = schema.getVectorInterface();
                    if (modelSourceType == DBAIModelSourceType.OBJECT_STORAGE) {
                        String modelLocation = AI_MODEL_SOURCE_LOCATION.of(input);
                        String credentialName = getCredentialName(input);
                        dataDefinition.createModelFromStorage(conn,
                                input.getSchemaName(true),
                                input.getAdjustedObjectName(),
                                modelLocation,
                                credentialName);

                    } else if (modelSourceType == DBAIModelSourceType.MODEL_FILE) {
                        Blob modelBlob = uploadOnnxModel(conn, input, progress);
                        dataDefinition.createModelFromFile(conn,
                                input.getSchemaName(true),
                                input.getAdjustedObjectName(),
                                modelBlob);

                    } else {
                        throw new IllegalArgumentException("Unsupported model source type: " + modelSourceType);
                    }
                });

        ObjectChangeEvent.notify(CREATE, AI_MODEL, connectionId, schemaId);
    }

    private static String getCredentialName(DBObjectSpec input) {
        return DBObjectRef.getQualifiedObjectName(AI_MODEL_CREDENTIAL.of(input));
    }

    private Blob uploadOnnxModel(
            DBNConnection conn,
            DBObjectSpec input,
            ProgressIndicator progress) throws SQLException {
        File modelFile = new File(AI_MODEL_SOURCE_LOCATION.of(input));
        long fileSize = modelFile.length();
        double totalMB = fileSize / (1024.0 * 1024.0);

        progress.setText(txt("prc.object.text.UploadingOnnxModel", modelFile.getName(), input.getSchema().getName(true), input.getObjectName()));
        progress.setIndeterminate(false);
        progress.setFraction(0.0);

        Blob modelBlob = conn.createBlob();

        try (RandomAccessFile randomFile = new RandomAccessFile(modelFile, "r");
             FileChannel fileChannel = randomFile.getChannel();
             OutputStream dbOutput = modelBlob.setBinaryStream(1)) {

            MappedByteBuffer mappedFile = fileChannel.map(FileChannel.MapMode.READ_ONLY, 0, fileSize);
            byte[] chunk = new byte[1024 * 1024];  // 1 MB buffer
            long bytesUploaded = 0;

            while (mappedFile.hasRemaining()) {
                // allow user to cancel
                progress.checkCanceled();

                int toRead = Math.min(chunk.length, mappedFile.remaining());
                mappedFile.get(chunk, 0, toRead);
                dbOutput.write(chunk, 0, toRead);

                bytesUploaded += toRead;
                double fraction = bytesUploaded / (double) fileSize;

                // update the progress bar
                progress.setFraction(fraction);
                progress.setText2(txt("prc.object.text.UploadedModelSize",
                        String.format("%.1f", bytesUploaded / (1024.0 * 1024.0)),
                        String.format("%.1f", totalMB)));
            }
        } catch (Throwable e) {
            Diagnostics.conditionallyLog(e);
            throw Exceptions.toSqlException(e);
        }

        // final update (100%)
        progress.setFraction(1.0);
        progress.setText(txt("prc.object.text.UploadComplete"));


        return modelBlob;
    }
}
