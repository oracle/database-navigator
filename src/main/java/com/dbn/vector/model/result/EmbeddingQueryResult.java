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

package com.dbn.vector.model.result;

import com.dbn.common.icon.Icons;
import com.dbn.vector.model.request.EmbeddingSourceQuery;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Arrays;

public class EmbeddingQueryResult extends EmbeddingResult<EmbeddingSourceQuery> {
    public EmbeddingQueryResult(EmbeddingSourceQuery source) {
        super(source);
        initSteps();
    }

    private void initSteps() {
        setSteps(new ArrayList<>(Arrays.asList(
                new StepResult(PipelineStep.EMBED)
        )));
    }

    @Override
    public String getPresentableSize() {
        return "";
    }

    @Override
    public String getIdentifier() {
        return "";
    }

    @Override
    public String getName() {
        return getSource().getSelectStatementPreview();
    }

    @Nullable
    @Override
    public Icon getIcon() {
        return Icons.FILE_SQL;
    }

    @Override
    public String getTooltip() {
        return getSource().getSelectStatement();
    }

    public String getSelectStatement() {
        return getSource().getSelectStatement();
    }
}
