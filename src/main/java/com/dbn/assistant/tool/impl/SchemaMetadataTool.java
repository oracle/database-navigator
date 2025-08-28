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

package com.dbn.assistant.tool.impl;

import com.dbn.assistant.tool.AssistantToolBase;
import com.dbn.assistant.tool.AssistantToolFactory.Definition;
import com.dbn.assistant.tool.AssistantToolFactoryBase;
import com.dbn.object.DBSchema;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.output.structured.Description;

import java.util.List;

import static com.dbn.assistant.tool.AssistantToolCategory.METADATA_PROVIDER;

@Description("Provides information about the database schemas and catalogs")
public class SchemaMetadataTool extends AssistantToolBase {

    @Definition(
            category = METADATA_PROVIDER,
            type = "SCHEMA_METADATA",
            impl = SchemaMetadataTool.class)
    public static class Factory extends AssistantToolFactoryBase<SchemaMetadataTool> {}

    @Tool("Lists database schema names")
    public List<String> listSchemaNames(boolean includeSystemSchemas) {
        List<DBSchema> schemas = getConnection().getObjectBundle().getSchemas();
        return getObjectNames(schemas, s -> includeSystemSchemas || !s.isSystemSchema());
    }
}
