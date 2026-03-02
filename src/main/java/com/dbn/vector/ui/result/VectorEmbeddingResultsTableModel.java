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

package com.dbn.vector.ui.result;

import com.dbn.common.ui.table.DBNDynamicTableModel;
import com.dbn.common.ui.table.DBNTableWithGutterModel;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.request.EmbeddingSourceType;
import com.dbn.vector.model.result.EmbeddingFileResult;
import com.dbn.vector.model.result.EmbeddingResult;
import lombok.Getter;

import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.vector.model.result.SourceStatus.FAILED;
import static com.intellij.ui.SimpleTextAttributes.ERROR_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;

@Getter
public class VectorEmbeddingResultsTableModel extends DBNDynamicTableModel<EmbeddingResult> implements DBNTableWithGutterModel<EmbeddingResult> {
    public VectorEmbeddingResultsTableModel(VectorEmbeddingResult result) {
        super(EmbeddingResult.class, result.getResults());
        EmbeddingSourceType sourceType = result.getSourceType();

        String sourceName = switch (sourceType) {
            case FILE_SYSTEM -> "File sources";
            case DATABASE_TABLE -> "Table sources";
            case DATABASE_QUERY -> "Query sources";
        };

        addColumn(sourceName, r -> r.getName()).withIcon(r -> r.getIcon()).
                withTooltip(r -> r.getTooltip());

        if (sourceType == EmbeddingSourceType.FILE_SYSTEM) {
            addColumn("Source size", r -> r.getPresentableSize());
            addColumn("File store ID", r -> ((EmbeddingFileResult) r).getFileStoreId());
        }

        addColumn("Rows embedded", r -> r.getRowsInserted());
        addColumn("Task duration", r -> presentableDuration(r.getDuration(), true));
        addColumn("Status", r -> r.getStatus()).
                withAttributes(r -> r.getStatus() == FAILED ?
                        ERROR_ATTRIBUTES :
                        REGULAR_ATTRIBUTES);
    }
}