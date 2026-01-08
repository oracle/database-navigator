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

@Getter
public enum HelpTopic implements Constant<HelpTopic> {
    DATABASE_BROWSER("database-browser.html"),
    EVENTS_MONITOR("events-monitor.html"),
    EXECUTION_ENGINE("execution-engine.html"),
    DATABASE_ASSISTANT("database-assistant.html"),
    VECTOR_TOOLBOX("vector-toolbox.html"),
    TABLE_EDITORS("table-editors.html"),
    SESSION_BROWSER("session-browser.html"),
    PROGRAM_EDITOR("program-editor.html"),
    DDL_FILE_EDITOR("ddl-file-editor.html"),
    SQL_CONSOLE("sql-editor.html"),
    METHOD_EXECUTION("executing-methods.html"),
    METHOD_EXECUTION_HISTORY("method-execution-history.html"),
    JAVA_EXECUTION("executing-java-programs.html"),
    JAVA_EXECUTION_HISTORY("java-execution-history.html"),
    JAVA_EXECUTION_WRAPPERS("creating-java-execution-wrappers.html"),
    //...
    ;

    public static final String ID_PREFIX = DatabaseNavigator.DBN_PLUGIN_ID + ".";
    private final String path;

    HelpTopic(String url) {
        this.path = url;
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
