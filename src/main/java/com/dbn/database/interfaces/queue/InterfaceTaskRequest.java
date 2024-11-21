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

package com.dbn.database.interfaces.queue;

import com.dbn.common.Priority;
import com.dbn.connection.ConnectionContext;
import com.dbn.connection.ConnectionId;
import com.dbn.connection.SchemaId;
import com.intellij.openapi.project.Project;
import lombok.Getter;

@Getter
public class InterfaceTaskRequest extends ConnectionContext{
    private final String title;
    private final String text;
    private final Priority priority;


    private InterfaceTaskRequest(Project project, String title, String text, Priority priority, ConnectionId connectionId, SchemaId schemaId) {
        super(project, connectionId, schemaId);
        this.title = title;
        this.text = text;
        this.priority = priority;
    }

    public static InterfaceTaskRequest create(Priority priority, String title, String text, Project project, ConnectionId connectionId, SchemaId schemaId) {
        return new InterfaceTaskRequest(project, title, text, priority, connectionId, schemaId);
    }
}
