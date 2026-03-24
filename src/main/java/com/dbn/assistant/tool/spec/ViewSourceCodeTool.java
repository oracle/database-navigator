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
import com.dbn.assistant.tool.impl.ViewSourceCodeToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.sql.SQLException;

import static com.dbn.assistant.tool.AssistantToolCategory.SOURCE_CODE_PROVIDER;
import static com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;
import static com.dbn.assistant.tool.AssistantToolType.VIEW_SOURCE_CODE;

@ToolSpec(
    category = SOURCE_CODE_PROVIDER,
    type = VIEW_SOURCE_CODE,
    name = "View source-code",
    description = "Source code for views, materialized views or json relational duality views")
public interface ViewSourceCodeTool extends AssistantTool {

    @FactorySpec(
        spec = ViewSourceCodeTool.class,
        impl = ViewSourceCodeToolImpl.class)
    class Factory extends AssistantToolFactoryBase<ViewSourceCodeTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "LOAD_VIEW_SOURCE_CODE")
    @UtilitySpec(
            name = "Load view source-code",
            description = "Loads the source code of a given view",
            summary = "%s.%s")
    ViewSourceCode loadViewSourceCode(
            @P("Schema name") String schemaName,
            @P("View name") String viewName) throws SQLException;



    @Data
    @Description("View source-code")
    class ViewSourceCode {
        @Description("View name")
        private String name;

        @Description("Code content (select statement)")
        private String code;
    }
}
