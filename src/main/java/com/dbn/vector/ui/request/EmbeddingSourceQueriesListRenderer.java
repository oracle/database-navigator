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

package com.dbn.vector.ui.request;

import com.dbn.common.icon.Icons;
import com.dbn.common.ui.list.ColoredListCellRenderer;
import com.dbn.vector.model.request.EmbeddingSourceQuery;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.JList;

import static com.intellij.ui.SimpleTextAttributes.ERROR_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES;
import static com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES;

public class EmbeddingSourceQueriesListRenderer extends ColoredListCellRenderer<EmbeddingSourceQuery> {
    @Override
    protected void customize(@NotNull JList<? extends EmbeddingSourceQuery> list, EmbeddingSourceQuery value, int index, boolean selected, boolean hasFocus) {
        if (value == null) {
            append("(null)", ERROR_ATTRIBUTES);
            return;
        }

        SimpleTextAttributes regularAttributes = list.isEnabled() ?
                REGULAR_ATTRIBUTES :
                GRAYED_ATTRIBUTES;

        append(value.getSelectStatementPreview(), regularAttributes);

        setIcon(Icons.FILE_SQL);
        setToolTipText(value.getSelectStatement());
    }
}
