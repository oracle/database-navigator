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

package com.dbn.assistant.tool.spec;

import com.dbn.assistant.tool.AssistantTool;
import com.dbn.assistant.tool.AssistantToolFactoryBase;
import com.dbn.assistant.tool.AssistantToolInfo.ToolSpec;
import com.dbn.assistant.tool.AssistantToolInfo.UtilitySpec;
import com.dbn.assistant.tool.impl.ConsoleEditorToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;

import static com.dbn.assistant.tool.AssistantToolCategory.IDE_ACTION_INVOKER;
import static com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;

@ToolSpec(
        category = IDE_ACTION_INVOKER,
        type = "SQL_CONSOLE_EDITORS",
        name = "SQL console editors",
        description = "IDE actions for opening and changing SQL consoles (terminals)")
public interface ConsoleEditorTool extends AssistantTool {

    @FactorySpec(
            spec = ConsoleEditorTool.class,
            impl = ConsoleEditorToolImpl.class)
    class Factory extends AssistantToolFactoryBase<ConsoleEditorTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "LIST_SQL_CONSOLE_NAMES")
    @UtilitySpec(
            name = "List SQL console names",
            description = "Lists the available console names")
    List<String> listSqlConsoleNames();

    @Tool(name = "GET_CURRENT_SQL_CONSOLE_NAME")
    @UtilitySpec(
            name = "Current SQL console name",
            description = "Returns the name of the currently selected SQL console (null if none selected)")
    String getCurrentConsoleName();


    @Tool(name = "LOAD_SQL_CONSOLE_CONTENT")
    @UtilitySpec(
            name = "Load SQL console content",
            description = "Loads the content of a given SQL console",
            summary = "%s")
    String loadSqlConsoleContent(
            @P("Console name") String consoleName);


    @Tool(name = "UPDATE_SQL_CONSOLE_CONTENT")
    @UtilitySpec(
            name = "Update SQL console content",
            description = "Updates the content of a given SQL console",
            summary = "%s")
    void updateSqlConsoleContent(
            @P("Console name") String consoleName,
            @P("Console content") String consoleContent);


    @Tool(name = "OPEN_SQL_CONSOLE")
    @UtilitySpec(
            name = "Open SQL console",
            description = "Opens the specified SQL console in the IDE",
            summary = "%s")
    void openSqlConsole(
            @P("Console name") String consoleName);


    @Tool(name = "OPEN_NEW_SQL_CONSOLE")
    @UtilitySpec(
            name = "Open new SQL console",
            description = "Creates a new SQL console with the given content and opens it the IDE (returns the name of the new SQL console)")
    String openNewSqlConsole(
            @P("Console content") String consoleContent);

}
