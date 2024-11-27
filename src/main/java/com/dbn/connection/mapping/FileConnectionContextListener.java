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

package com.dbn.connection.mapping;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.SchemaId;
import com.dbn.connection.session.DatabaseSession;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.Topic;

import java.util.EventListener;

public interface FileConnectionContextListener extends EventListener {
    Topic<FileConnectionContextListener> TOPIC = Topic.create("Connection mapping changed", FileConnectionContextListener.class);

    default void connectionChanged(Project project, VirtualFile file, ConnectionHandler connection){
        mappingChanged(project, file);
    }

    default void schemaChanged(Project project, VirtualFile file, SchemaId schema){
        mappingChanged(project, file);
    }

    default void sessionChanged(Project project, VirtualFile file, DatabaseSession session){
        mappingChanged(project, file);
    }

    default void mappingChanged(Project project, VirtualFile file){}
}
