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
import com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;
import com.dbn.assistant.tool.AssistantToolInfo.UtilitySpec;
import com.dbn.assistant.tool.impl.JavaSourceCodeToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import java.sql.SQLException;

import static com.dbn.assistant.tool.AssistantToolCategory.SOURCE_CODE_PROVIDER;
import static com.dbn.assistant.tool.AssistantToolType.JAVA_SOURCE_CODE;

@ToolSpec(
        category = SOURCE_CODE_PROVIDER,
        type = JAVA_SOURCE_CODE,
        name = "Java source-code",
        description = "Source code for OJVM object (Java Class, Java Resource)")
public interface JavaSourceCodeTool extends AssistantTool {

    @FactorySpec(
            spec = JavaSourceCodeTool.class,
            impl = JavaSourceCodeToolImpl.class)
    class Factory extends AssistantToolFactoryBase<JavaSourceCodeTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "LOAD_JAVA_CLASS_CODE")
    @UtilitySpec(
            name = "Load Java class code",
            description = "Loads the source code of a given java class",
            summary = "%s.%s")
    JavaSourceCodeTool.JavaSourceCode loadJavaClassCode(
            @P("Schema name") String schemaName,
            @P("Java class name") String className) throws SQLException;


    @Tool(name = "LOAD_JAVA_RESOURCE_CODE")
    @UtilitySpec(
            name = "Load Java resource source-code",
            description = "Loads the source code of a given user-defined java resource",
            summary = "%s.%s")
    JavaSourceCodeTool.JavaSourceCode loadJavaResourceCode(
            @P("Schema name") String schemaName,
            @P("Java resource name") String resourceName) throws SQLException;


    @Data
    @Description("Java source-code")
    class JavaSourceCode {
        @Description("Java object name")
        private String name;

        @Description("Java object type")
        private String type;

        @Description("Java code")
        private String code;
    }
}
