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
import com.dbn.ml.onnx.OnnxMetadataHelper;
import com.dbn.object.DBSchema;
import com.dbn.object.event.ObjectChangeEvent;
import com.dbn.object.factory.ObjectFactoryAdapter;
import com.dbn.object.factory.model.DBAIModelSpec;
import com.dbn.object.factory.ui.DBAIModelFactoryInputForm;
import com.dbn.object.type.DBObjectType;
import com.dbn.vector.common.ModelSourceType;
import com.intellij.openapi.progress.ProgressIndicator;

import java.io.File;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.Priority.MEDIUM;
import static com.dbn.object.event.ObjectChangeAction.CREATE;
import static com.dbn.object.type.DBObjectType.AI_MODEL;

public class DBAIModelFactoryAdapter implements ObjectFactoryAdapter<DBAIModelSpec, DBAIModelFactoryInputForm> {

    @Override
    public DBObjectType getObjectType() {
        return AI_MODEL;
    }

    @Override
    public DBAIModelSpec createInput(DBSchema schema) {
        return new DBAIModelSpec(schema);
    }

    public DBAIModelFactoryInputForm createInputForm(DBNComponent parent, DBAIModelSpec input) {
        return new DBAIModelFactoryInputForm(parent, input);
    }

    @Override
    public void validateInput(DBAIModelSpec input, List<String> errors) {
        // TODO
    }

    @Override
    public void createObject(DBAIModelSpec input) throws SQLException {
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
                conn -> {
                    DatabaseVectorInterface dataDefinition = schema.getVectorInterface();
                    if (modelSourceType == ModelSourceType.OBJECT_STORAGE) {
                        dataDefinition.createModelFromStorage(conn,
                                input.getSchemaName(true),
                                input.getObjectName(true),
                                input.getSourceLocation(),
                                input.getCredentialName());

                    } else if (modelSourceType == ModelSourceType.MODEL_FILE) {
                        // Look for Oracle metadata sidecar JSON file
                        File modelFile = new File(input.getSourceLocation());
                        String oracleMetadata = null;
                        try {
                            oracleMetadata = OnnxMetadataHelper.readMetadataFile(modelFile.toPath());
                        } catch (Exception e) {
                            Diagnostics.conditionallyLog(e);
                            // Continue without metadata - will use default
                        }
                        
                        Blob modelBlob = uploadOnnxModel(conn, input, progress);
                        dataDefinition.createModelFromFile(conn,
                                input.getSchemaName(true),
                                input.getObjectName(true),
                                modelBlob,
                                oracleMetadata);

                    } else {
                        throw new IllegalArgumentException("Unsupported model source type: " + modelSourceType);
                    }
                });

        ObjectChangeEvent.notify(CREATE, AI_MODEL, connectionId, schemaId);
    }

    private Blob uploadOnnxModel(
            DBNConnection conn,
            DBAIModelSpec input,
            ProgressIndicator progress) throws SQLException {
        File modelFile = new File(input.getSourceLocation());
        long fileSize = modelFile.length();
        double totalMB = fileSize / (1024.0 * 1024.0);

        // Tell the ProgressIndicator what we're doing
        progress.setText("Uploading ONNX model \"" + modelFile.getName() + "\" as " + input.getSchema().getName(true) + ".\"" + input.getObjectName() + "\"");
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
                progress.setText2(String.format(
                        "Uploaded %.1f MB of %.1f MB",
                        bytesUploaded / (1024.0 * 1024.0),
                        totalMB
                ));
            }
        } catch (Throwable e) {
            Diagnostics.conditionallyLog(e);
            throw Exceptions.toSqlException(e);
        }

        // final update (100%)
        progress.setFraction(1.0);
        progress.setText("Upload complete");


        return modelBlob;
    }
}
