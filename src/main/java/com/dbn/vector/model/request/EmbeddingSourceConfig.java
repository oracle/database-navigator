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

import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;

@Getter
@Setter
public final class EmbeddingSourceConfig extends EmbeddingRequestConfig {
    private EmbeddingSourceType sourceType = EmbeddingSourceType.DATABASE_TABLE;

    // transient single selection configs
    private final EmbeddingSourceTable sourceTable = new EmbeddingSourceTable(); 
    private final EmbeddingSourceQuery sourceQuery = new EmbeddingSourceQuery();
    
    private final EmbeddingSourceTables sourceTables = new EmbeddingSourceTables();
    private final EmbeddingSourceQueries sourceQueries = new EmbeddingSourceQueries();
    private final EmbeddingSourceFiles sourceFiles = new EmbeddingSourceFiles();

    @Override
    public void readState(Element element) {
        if (element == null) return;

        super.readState(element);
        sourceType = enumAttribute(element, "source-type", sourceType);

        sourceTable.readState(element.getChild("single-table-source"));
        sourceQuery.readState(element.getChild("single-query-source"));

        sourceTables.readState(element);
        sourceQueries.readState(element);
        sourceFiles.readState(element);
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);
        setEnumAttribute(element, "source-type", sourceType);

        sourceTable.writeState(newElement(element,  "single-table-source"));
        sourceQuery.writeState(newElement(element,  "single-query-source"));

        sourceQueries.writeState(element);
        sourceTables.writeState(element);
        sourceFiles.writeState(element);
    }

    public int getRecordCount() {
        return switch (sourceType) {
            case DATABASE_TABLE -> sourceTables.size();
            case DATABASE_QUERY -> sourceQueries.size();
            case FILE_SYSTEM -> sourceFiles.size();
        };
    }
}
