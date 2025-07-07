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

package com.dbn.event.registration.model;

import com.dbn.common.project.ProjectRef;
import com.intellij.openapi.project.Project;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
public class DataChangeRegistration {
    private transient final ProjectRef project;

    private final String userName;
    private final Long regId;
    private final int regFlags;
    private final String callback;
    private final int operationsFilter;
    private final int changeLag;
    private final long timeout;
    private final String tableName;
    private boolean active;
    //todo maybe add cloumn for the option this registration is on.

    public DataChangeRegistration(
            Project project,
            String userName, Long regId, int regFlags, String callback,
                                  int operationsFilter, int changeLag,
                                  long timeout, String tableName) {
        this.project = ProjectRef.of(project);
        this.userName = userName;
        this.regId = regId;
        this.regFlags = regFlags;
        this.callback = callback;
        this.operationsFilter = operationsFilter;
        this.changeLag = changeLag;
        this.timeout = timeout;
        this.tableName = tableName;
    }

    @NotNull
    public Project getProject() {
        return ProjectRef.ensure(project);
    }

    public String getOperationsDescription() {
        if (operationsFilter == 0) {
            return "ALL OPERATIONS";
        }
        StringBuilder sb = new StringBuilder();
        if ((operationsFilter & 0x2) != 0) sb.append("INSERT, ");
        if ((operationsFilter & 0x4) != 0) sb.append("UPDATE, ");
        if ((operationsFilter & 0x8) != 0) sb.append("DELETE, ");
        if (sb.length() > 0) sb.setLength(sb.length() - 2); // Remove trailing comma and space
        return sb.toString();
    }


}