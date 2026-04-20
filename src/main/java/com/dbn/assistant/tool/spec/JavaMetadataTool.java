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
import com.dbn.assistant.tool.impl.JavaMetadataToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;

import static com.dbn.assistant.tool.AssistantToolCategory.METADATA_PROVIDER;
import static com.dbn.assistant.tool.AssistantToolType.JAVA_METADATA;

@ToolSpec(
        category = METADATA_PROVIDER,
        type = JAVA_METADATA,
        name = "Java metadata",
        description = "Information about java units (classes, resources) in a given schema")
public interface JavaMetadataTool extends AssistantTool {

    @FactorySpec(
            spec = JavaMetadataTool.class,
            impl = JavaMetadataToolImpl.class)
    class Factory extends AssistantToolFactoryBase<JavaMetadataTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "LIST_JAVA_CLASS_NAMES")
    @UtilitySpec(
            name = "List class names",
            description = "Lists the names of class in a given schema",
            summary = "schema %s")
    List<String> listClassNames(@P("Schema name") String schemaName);


    @Tool(name = "LIST_JAVA_RESOURCE_NAMES")
    @UtilitySpec(
            name = "List resource names",
            description = "Lists the names of resources in a given schema",
            summary = "schema %s")
    List<String> listResourceNames(@P("Schema name") String schemaName);
}
