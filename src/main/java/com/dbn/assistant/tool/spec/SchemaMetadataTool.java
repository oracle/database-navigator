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
import com.dbn.assistant.tool.AssistantToolInfo.FactorySpec;
import com.dbn.assistant.tool.AssistantToolInfo.ToolSpec;
import com.dbn.assistant.tool.AssistantToolInfo.UtilitySpec;
import com.dbn.assistant.tool.impl.SchemaMetadataToolImpl;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.util.List;

import static com.dbn.assistant.tool.AssistantToolCategory.METADATA_PROVIDER;

@ToolSpec(
        category = METADATA_PROVIDER,
        type = "SCHEMA_METADATA",
        name = "Schema metadata",
        description = "Information about database schemas and catalogs")
public interface SchemaMetadataTool extends AssistantTool {

    @FactorySpec(
        spec = SchemaMetadataTool.class,
        impl = SchemaMetadataToolImpl.class)
    class Factory extends AssistantToolFactoryBase<SchemaMetadataTool> {}

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool(name = "LIST_SCHEMA_NAMES")
    @UtilitySpec(
            name = "List schema names",
            description = "Lists database schema names")
    List<String> listSchemaNames(@P("Include system schemas") boolean includeSystemSchemas);
}
