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
    private final EmbeddingTableSource sourceTable = new EmbeddingTableSource(); // transient single selection
    private final EmbeddingTableSources sourceTables = new EmbeddingTableSources();
    private final EmbeddingFileSources sourceFiles = new EmbeddingFileSources();

    @Override
    public void readState(Element element) {
        if (element == null) return;

        super.readState(element);
        sourceType = enumAttribute(element, "source-type", sourceType);

        sourceTable.readState(element.getChild("single-table-source"));
        sourceTables.readState(element);
        sourceFiles.readState(element);
    }

    @Override
    public void writeState(Element element) {
        super.writeState(element);
        setEnumAttribute(element, "source-type", sourceType);

        sourceTable.writeState(newElement(element,  "single-table-source"));
        sourceTables.writeState(element);
        sourceFiles.writeState(element);
    }

    public int getRecordCount() {
        return switch (sourceType) {
            case DATABASE_TABLE -> sourceTables.size();
            case FILE_SYSTEM -> sourceFiles.size();
        };
    }
}
