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

package com.dbn.vector.ui.source;

import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.vector.model.VectorEmbeddingRequest;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

public class EmbeddingSourceTableDialog extends DBNDialog<EmbeddingSourceTableForm> {
    private final VectorEmbeddingRequest request;

    public EmbeddingSourceTableDialog(VectorEmbeddingRequest request) {
        super(request.getProject(), "Select Source Table", false);
        this.request = request;

        init();
    }


    @Override
    protected @NotNull EmbeddingSourceTableForm createForm() {
        return new EmbeddingSourceTableForm(this, request);
    }

    @Override
    @SneakyThrows
    protected void doOKAction() {
        applyFormChanges();
        super.doOKAction();
    }

    @Override
    @SneakyThrows
    public void doCancelAction() {
        applyFormChanges(); // preserve input even if canceled
        super.doCancelAction();
    }
}
