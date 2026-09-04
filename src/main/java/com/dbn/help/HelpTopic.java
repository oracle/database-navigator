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

package com.dbn.help;

import com.dbn.DatabaseNavigator;
import com.dbn.common.constant.Constant;
import com.dbn.common.constant.Constants;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;

@Getter
public enum HelpTopic implements Constant<HelpTopic> {
    INTRODUCTION("introduction-oracle-database-navigator.html"),
    DATABASE_BROWSER("database-browser.html"),
    EVENTS_MONITOR("events-monitor.html"),
    EVENTS_REGISTRATION("enabling-notification-data-change.html"),
    EXECUTION_ENGINE("execution-engine.html"),
    DATABASE_ASSISTANT("database-assistant.html"),
    DATABASE_ASSISTANT_TOOLS("tool-approvals-and-tool-data.html"),
    DATABASE_ASSISTANT_SELECT_AI("oracle-select-ai.html"),
    VECTOR_TOOLBOX("vector-toolbox.html"),
    VECTOR_SEARCH("vector-toolbox.html"), // TODO point to rag documentation
    TABLE_EDITORS("table-editors.html"),
    SESSION_BROWSER("session-browser.html"),
    JAVA_EDITOR("java-editor.html"),
    PROGRAM_EDITOR("program-editor.html"),
    DDL_FILE_EDITOR("ddl-file-editor.html"),
    DDL_FILE_ASSOCIATION("creating-ddl-files.html"),
    DDL_FILE_DISASSOCIATION("disassociating-ddl-files.html"),
    SQL_CONSOLE("sql-editor.html"),
    RESOURCE_MONITOR("resource-monitor.html"),
    METHOD_DEBUGGING("debugging-methods.html"),
    METHOD_EXECUTION("executing-methods.html"),
    METHOD_EXECUTION_HISTORY("method-execution-history.html"),
    JAVA_DEBUGGING("debugging-java-programs.html"),
    JAVA_EXECUTION("executing-java-programs.html"),
    JAVA_EXECUTION_HISTORY("java-execution-history.html"),
    JAVA_EXECUTION_WRAPPERS("creating-java-execution-wrappers.html"),
    SCRIPT_EXECUTION("executing-scripts.html"),
    RECORD_VIEWER("viewing-database-records.html"),
    RECORD_EDITOR("editing-database-records.html"),

    // config
    DATABASE_CONFIG_PROPERTIES("connection-properties.html"),
    DATABASE_CONFIG_DETAILS("connection-details.html"),
    DATABASE_CONFIG("database-configuration.html"),
    ASSISTANT_CONFIG("creating-credentials-public-llms.html"),
    DATA_GRID_SETTINGS("data-grid-settings.html"),
    DATA_EDITOR_SETTINGS("data-editor-settings.html"),
    CODE_EDITOR_SETTINGS("configuration-code-editors.html"),
    EXECUTION_ENGINE_SETTINGS("working-execution-engine.html"),
    OPERATIONS_SETTINGS(null),
    DDL_FILE_SETTINGS("ddl-file-management.html"),
    TRANSACTION_HANDLING("transaction-handling.html"),
    GENERAL_SETTINGS("configuration-database-environments.html"),
    SCRIPT_EXECUTION_SETUP("creating-command-line-interfaces-databases.html"),
    DATABASE_ASSISTANT_AI_PROFILES("creating-ai-profiles-oracle-select-ai.html"),
    DATABASE_ASSISTANT_CREDENTIALS("creating-credentials-oracle-select-ai.html"),
    MCP_SERVER_BUILDER("building-mcp-server.html"),
    //...
    ;

    public static final String ID_PREFIX = DatabaseNavigator.DBN_PLUGIN_ID + ".";
    private final String path;

    HelpTopic(@NonNls String path) {
        this.path = path;
    }

    public String asHelpTopicId() {
        return ID_PREFIX + id();
    }

    public static HelpTopic get(String helpTopicId) {
        if (helpTopicId.startsWith(ID_PREFIX)) {
            helpTopicId = helpTopicId.substring(ID_PREFIX.length());
        }
        return Constants.get(HelpTopic.values(), helpTopicId);
    }




}
