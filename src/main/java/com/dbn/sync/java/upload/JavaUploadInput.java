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

package com.dbn.sync.java.upload;

import com.dbn.batch.impl.BatchInputBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
@Setter
public class JavaUploadInput extends BatchInputBase<JavaUploadTask> {

    private VirtualFile rootFile;
    private ConnectionRef targetConnection;
    private DBObjectRef<DBSchema> targetSchema;

    private List<String> dependentObjects;


    public JavaUploadInput(Project project, VirtualFile rootFile, List<JavaUploadTask> tasks) {
        super(project, tasks);
        this.rootFile = rootFile;
    }

    @Nullable
    public String getTargetSchemaName() {
        return targetSchema == null ? null : targetSchema.getObjectName();
    }

    @Nullable
    public String getTargetConnectionName() {
        return targetConnection == null ? null : targetConnection.ensure().getName();
    }



    @Nullable
    public DBSchema getTargetSchema() {
        return DBObjectRef.get(targetSchema);
    }

    @Nullable
    public ConnectionHandler getTargetConnection() {
        return ConnectionRef.get(targetConnection);
    }

    public void setTargetConnection(ConnectionHandler targetConnection) {
        this.targetConnection = ConnectionRef.of(targetConnection);
    }

    public void setTargetSchema(DBSchema targetSchema) {
        this.targetSchema = DBObjectRef.of(targetSchema);
    }

    public DatabaseContext getDatabaseContext() {
        return getTargetConnection();
    }

}

