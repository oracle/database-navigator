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

import com.dbn.common.color.Colors;
import com.dbn.common.ui.table.DBNDynamicTableModel;
import com.dbn.common.ui.table.DBNTableWithGutterModel;
import com.dbn.vector.model.VectorEmbeddingResult;
import com.dbn.vector.model.request.EmbeddingSourceType;
import com.dbn.vector.model.result.EmbeddingFileResult;
import com.dbn.vector.model.result.EmbeddingResult;
import com.intellij.ui.SimpleTextAttributes;
import lombok.Getter;

import static com.dbn.common.task.TaskStatus.SKIPPED;
import static com.dbn.common.util.TimeUtil.presentableDuration;
import static com.dbn.nls.NlsResources.txt;
import static com.intellij.ui.SimpleTextAttributes.ERROR_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.GRAY_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.STYLE_PLAIN;

@Getter
public class VectorEmbeddingResultsTableModel extends DBNDynamicTableModel<EmbeddingResult> implements DBNTableWithGutterModel<EmbeddingResult> {
    public VectorEmbeddingResultsTableModel(VectorEmbeddingResult result) {
        super(EmbeddingResult.class, result.getResults());
        EmbeddingSourceType sourceType = result.getSourceType();

        String sourceName = switch (sourceType) {
            case FILE_SYSTEM -> txt("msg.vector.column.FileSources");
            case DATABASE_TABLE -> txt("msg.vector.column.TableSources");
            case DATABASE_QUERY -> txt("msg.vector.column.QuerySources");
        };

        addColumn(sourceName, r -> r.getName()).withIcon(r -> r.getIcon()).
                withTooltip(r -> r.getSourceTooltip()).
                withAttributes(r -> attributes(r));

        if (sourceType == EmbeddingSourceType.FILE_SYSTEM) {
            addColumn(txt("msg.vector.column.SourceSize"), r -> r.getPresentableSize()).
                    withAttributes(r -> attributes(r));
            addColumn(txt("msg.vector.column.FileStoreId"), r -> ((EmbeddingFileResult) r).getFileStoreId()).
                    withAttributes(r -> attributes(r));
        }

        addColumn(txt("msg.vector.column.RowsEmbedded"), r -> r.getRowsInserted()).
                withAttributes(r -> attributes(r));

        addColumn(txt("msg.vector.column.TaskDuration"), r -> presentableDuration(r.getDuration(), true)).
                withAttributes(r -> attributes(r));

        addColumn(txt("app.shared.column.Status"), r -> r.getStatus()).
                withAttributes(r -> statusAttributes(r)).
                withTooltip(r -> r.getStatusTooltip());

        addColumn(txt("msg.vector.column.StatusMessage"), r -> r.getStatusMessage()).
                withAttributes(r -> attributes(r));
    }

    private static SimpleTextAttributes attributes(EmbeddingResult r) {
        return r.getStatus() == SKIPPED ? GRAY_ATTRIBUTES : REGULAR_ATTRIBUTES;
    }
    private static SimpleTextAttributes statusAttributes(EmbeddingResult r) {
        return switch (r.getStatus()) {
            case SKIPPED -> GRAY_ATTRIBUTES;
            case FAILED -> ERROR_ATTRIBUTES;
            case DONE -> new SimpleTextAttributes(STYLE_PLAIN, Colors.getLabelSuccessForeground());
            default -> REGULAR_ATTRIBUTES;
        };
    }
}
