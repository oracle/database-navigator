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
import com.dbn.assistant.tool.AssistantTool.Definition;
import com.dbn.assistant.tool.AssistantToolFactoryBase;
import com.dbn.assistant.tool.impl.DatabaseMetadataToolImpl;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.model.output.structured.Description;
import lombok.Data;

import static com.dbn.assistant.tool.AssistantToolCategory.METADATA_PROVIDER;

@Description("Provides information about the database type, name and version")
@Definition(type = "DATABASE_METADATA", category = METADATA_PROVIDER, impl = DatabaseMetadataToolImpl.class)
public interface DatabaseMetadataTool extends AssistantTool {

    class Factory extends AssistantToolFactoryBase<DatabaseMetadataTool> {
        public Factory() {
            super(DatabaseMetadataTool.class);
        }
    }

    /*********************************************
     *                 TOOLS                     *
     *********************************************/

    @Tool("Loads database information")
    DatabaseInformation loadDatabaseInformation();

    @Data
    class DatabaseInformation {
        private String type;
        private String name;
        private String version;

    }
}
