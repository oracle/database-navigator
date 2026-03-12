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

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Cloneable;
import lombok.Data;
import lombok.SneakyThrows;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.setStringAttribute;
import static com.dbn.common.options.setting.Settings.stringAttribute;
import static com.dbn.common.util.Unsafe.cast;
import static com.dbn.vector.model.request.EmbeddingSourceType.DATABASE_TABLE;

@Data
public class EmbeddingSourceTable implements EmbeddingSource, PersistentStateElement, Cloneable<EmbeddingSourceTable> {
    private String schemaName;
    private String tableName;
    private String keyColumnName;
    private String dataColumnName;

    @Override
    public EmbeddingSourceType getType() {
        return DATABASE_TABLE;
    }

    @Override
    public String getIdentifier() {
        return schemaName + "." + tableName + "#" + keyColumnName + "/" + dataColumnName;
    }

    @Override
    public void readState(Element element) {
        if (element == null) return;

        schemaName = stringAttribute(element, "schema");
        tableName = stringAttribute(element, "table");
        keyColumnName = stringAttribute(element, "key-column");
        dataColumnName = stringAttribute(element, "data-column");
    }

    @Override
    public void writeState(Element element) {
        setStringAttribute(element, "schema", schemaName);
        setStringAttribute(element, "table", tableName);
        setStringAttribute(element, "key-column", keyColumnName);
        setStringAttribute(element, "data-column", dataColumnName);
    }

    @Override
    @SneakyThrows
    public EmbeddingSourceTable clone() {
        return cast(super.clone());
    }
}
