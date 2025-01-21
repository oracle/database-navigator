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

package com.dbn.execution.logging;

import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.jdbc.DBNConnection;
import com.dbn.database.DatabaseFeature;
import com.dbn.database.interfaces.DatabaseCompatibilityInterface;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;

import static com.dbn.common.component.Components.projectService;
import static com.dbn.common.notification.NotificationGroup.LOGGING;
import static com.dbn.diagnostics.Diagnostics.conditionallyLog;
import static com.dbn.nls.NlsResources.txt;

@Slf4j
public class DatabaseLoggingManager extends ProjectComponentBase {

    public static final String COMPONENT_NAME = "DBNavigator.Project.DatabaseLoggingManager";

    private DatabaseLoggingManager(Project project) {
        super(project, COMPONENT_NAME);
    }

    public static DatabaseLoggingManager getInstance(@NotNull Project project) {
        return projectService(project, DatabaseLoggingManager.class);
    }

    /*********************************************************
     *                       Custom                          *
     *********************************************************/
    public boolean enableLogger(ConnectionHandler connection, DBNConnection conn) {
        if (!DatabaseFeature.DATABASE_LOGGING.isSupported(connection)) return false;

        try {
            DatabaseMetadataInterface metadata = connection.getMetadataInterface();
            metadata.enableLogger(conn);
            return true;
        } catch (SQLException e) {
            conditionallyLog(e);
            log.warn("Error enabling database logging: {}", e.getMessage());
            String logName = getLogName(connection);
            sendWarningNotification(LOGGING, txt("ntf.logging.error.FailedToEnableLogging", logName, e));
            return false;
        }
    }

    public void disableLogger(ConnectionHandler connection, @Nullable DBNConnection conn) {
        if (conn == null)  return;
        if (!DatabaseFeature.DATABASE_LOGGING.isSupported(connection)) return;

        try {
            DatabaseMetadataInterface metadata = connection.getMetadataInterface();
            metadata.disableLogger(conn);
        } catch (SQLException e) {
            conditionallyLog(e);
            log.warn("Error disabling database logging: {}", e.getMessage());
            String logName = getLogName(connection);
            sendWarningNotification(LOGGING, txt("ntf.logging.error.FailedToDisableLogging", logName, e));
        }
    }

    public String readLoggerOutput(ConnectionHandler connection, DBNConnection conn) {
        try {
            DatabaseMetadataInterface metadata = connection.getMetadataInterface();
            return metadata.readLoggerOutput(conn);
        } catch (SQLException e) {
            conditionallyLog(e);
            log.warn("Error reading database log output: {}", e.getMessage());
            String logName = getLogName(connection);
            sendWarningNotification(LOGGING, txt("ntf.logging.error.FailedToLoadLogContent", logName, e));
        }

        return null;
    }

    @NotNull
    private String getLogName(ConnectionHandler connection) {
        DatabaseCompatibilityInterface compatibility = connection.getCompatibilityInterface();
        String logName = compatibility.getDatabaseLogName();
        if (Strings.isEmpty(logName)) {
            logName = txt("app.logging.label.LogName_GENERIC");
        }
        return logName;
    }

    public boolean supportsLogging(ConnectionHandler connection) {
        return DatabaseFeature.DATABASE_LOGGING.isSupported(connection);
    }

}
