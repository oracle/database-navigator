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

package com.dbn.vector.model.request;

import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;

@Getter
@Setter
public class EmbeddingStagingConfig extends EmbeddingRequestConfig {
    private String schemaName;
    private String tableName;

    public String getQualifiedTableName() {
        return schemaName + "." + tableName;
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        super.readState(element);
        schemaName = stringAttribute(element, "schema");
        tableName = stringAttribute(element, "table");
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);
        setStringAttribute(element, "schema", schemaName);
        setStringAttribute(element, "table", tableName);
    }
}
