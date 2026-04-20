/*
 * Copyright 2026 Oracle and/or its affiliates
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
import com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;
import com.dbn.assistant.tool.impl.JavaCodeEditorToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import static com.dbn.assistant.tool.AssistantToolCategory.IDE_ACTION_INVOKER;
import static com.dbn.assistant.tool.AssistantToolType.JAVA_CODE_EDITORS;

@ToolSpec(
        category = IDE_ACTION_INVOKER,
        type = JAVA_CODE_EDITORS,
        name = "Source-code editors",
        description = "IDE actions for editing source-code of database objects")
public interface JavaCodeEditorTool extends AssistantTool {

    @FactorySpec(
            spec = JavaCodeEditorTool.class,
            impl = JavaCodeEditorToolImpl.class)
    class Factory extends AssistantToolFactoryBase<JavaCodeEditorTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "OPEN_JAVA_CLASS_EDITOR")
    @UtilitySpec(
            name = "Open program code editor",
            description = "Opens the code editor of a given OJVM Java class in the IDE",
            summary = "%s.%s")
    void openJavaClassEditor(
            @P("Schema name") String schemaName,
            @P("Class name") String className);


    @Tool(name = "OPEN_JAVA_RESOURCE_EDITOR")
    @UtilitySpec(
            name = "Open type code editor",
            description = "Opens the code editor of a given OJVM Java resource in the IDE",
            summary = "%s.%s")
    void openJavaResourceEditor(
            @P("Schema name") String schemaName,
            @P("Resource name") String resourceName);


}
