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

package com.dbn.common.operation;

import com.dbn.common.constant.Constant;
import com.dbn.connection.context.DatabaseContext;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.prerequisite.DatabasePrerequisiteManager;
import com.intellij.openapi.project.Project;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import static com.dbn.database.DatabaseFeature.DATA_CHANGE_NOTIFICATION;
import static com.dbn.database.DatabaseFeature.CONNECTION_CONFIGURATION;
import static com.dbn.database.DatabaseFeature.DEBUGGING;
import static com.dbn.database.DatabaseFeature.JAVA_VIRTUAL_MACHINE;
import static com.dbn.database.DatabaseFeature.VECTOR_EMBEDDING;
import static com.dbn.nls.NlsResources.txt;

@Getter
public enum DatabaseOperation implements Constant<DatabaseOperation> {

    CHANGE_JAVA_CODE(
            JAVA_VIRTUAL_MACHINE,
            txt("app.shared.const.Operation_CHANGE_JAVA_CODE"),
            txt("msg.prerequisites.warning.MissingPrerequisites_CHANGE_JAVA_CODE")),

    EXECUTE_JAVA_CODE(
            JAVA_VIRTUAL_MACHINE,
            txt("app.shared.const.Operation_EXECUTE_JAVA_CODE"),
            txt("msg.prerequisites.warning.MissingPrerequisites_EXECUTE_JAVA_CODE")),

    CREATE_JAVA_WRAPPER(
            JAVA_VIRTUAL_MACHINE,
            txt("app.shared.const.Operation_CREATE_JAVA_WRAPPER"),
            txt("msg.prerequisites.warning.MissingPrerequisites_CREATE_JAVA_WRAPPER")),

    DEBUG_JAVA_CODE(
            JAVA_VIRTUAL_MACHINE, txt("app.shared.const.Operation_DEBUG_JAVA_CODE"),
            txt("msg.prerequisites.warning.MissingPrerequisites_DEBUG_JAVA_CODE")),

    DEBUG_PLSQL_CODE_JDBC(
            DEBUGGING,
            txt("app.shared.const.Operation_DEBUG_PLSQL_CODE_JDBC"),
            txt("msg.prerequisites.warning.MissingPrerequisites_DEBUG_PLSQL_CODE_JDBC")),

    DEBUG_PLSQL_CODE_JDWP(
            DEBUGGING,
            txt("app.shared.const.Operation_DEBUG_PLSQL_CODE_JDWP"),
            txt("msg.prerequisites.warning.MissingPrerequisites_DEBUG_PLSQL_CODE_JDWP")),

    ENABLE_CHANGE_NOTIFICATIONS(
            DATA_CHANGE_NOTIFICATION,
            txt("app.shared.const.Operation_ENABLE_CHANGE_NOTIFICATIONS"),
            txt("msg.prerequisites.warning.MissingPrerequisites_ENABLE_CHANGE_NOTIFICATIONS")),

    CREATE_CONNECTION_CONFIGURATION(
            CONNECTION_CONFIGURATION,
            txt("app.shared.const.Operation_CREATE_CONNECTION_CONFIGURATION"),
            txt("msg.prerequisites.warning.MissingPrerequisites_CREATE_CONNECTION_CONFIGURATION")),

    CREATE_VECTOR_EMBEDDINGS(
            VECTOR_EMBEDDING,
            txt("app.shared.const.Operation_CREATE_VECTOR_EMBEDDINGS"),
            txt("msg.prerequisites.warning.MissingPrerequisites_CREATE_VECTOR_EMBEDDINGS")),

    MANAGE_CONNECTION_CONFIGURATIONS(
            CONNECTION_CONFIGURATION,
            txt("app.shared.const.Operation_MANAGE_CONNECTION_CONFIGURATIONS"),
            txt("msg.prerequisites.warning.MissingPrerequisites_MANAGE_CONNECTION_CONFIGURATIONS")),

    ;

    private final DatabaseFeature feature;
    private final String name;
    //private final String description;
    private final String missingPrerequisiteMessage;

    DatabaseOperation(DatabaseFeature feature, String name, /*String description, */String missingPrerequisiteMessage) {
        this.feature = feature;
        this.name = name;
        //this.description = description;
        this.missingPrerequisiteMessage = missingPrerequisiteMessage;
    }

    @Override
    public String toString() {
        return name;
    }

    public void start(DatabaseContext context, Runnable runnable) {
        Project project = context.ensureConnection().getProject();
        DatabasePrerequisiteManager prerequisiteManager = DatabasePrerequisiteManager.getInstance(project);
        prerequisiteManager.startOperation(context, this, runnable);
    }

    public boolean isSupported(@Nullable DatabaseContext context) {
        if (context == null) return false;
        DatabaseCompatibilityInterface compatibility = context.getCompatibilityInterface();
        return compatibility.supportsOperation(this);
    }
}
