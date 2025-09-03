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
import com.dbn.assistant.tool.spec.SchemaMetadataTool;
import com.dbn.object.DBSchema;

import java.util.List;

public class SchemaMetadataToolImpl extends AssistantToolBase implements SchemaMetadataTool {

    @Override
    public List<String> listSchemaNames(boolean includeSystemSchemas) {
        List<DBSchema> schemas = getConnection().getObjectBundle().getSchemas();
        return getObjectNames(schemas, false, s -> includeSystemSchemas || !s.isSystemSchema());
    }
}
