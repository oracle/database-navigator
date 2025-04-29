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

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.sync.common.impl.SyncInputBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JavaUploadInput extends SyncInputBase<JavaUploadElement> {

    private VirtualFile rootFile;
    private ConnectionHandler connection;
    private String schemaName;

    private List<String> dependentObjects;


    public JavaUploadInput(Project project, VirtualFile rootFile, List<JavaUploadElement> elements) {
        super(project);
        this.rootFile = rootFile;
        addElements(elements);
    }

    public DatabaseContext getDatabaseContext() {
        return connection;
    }

}

