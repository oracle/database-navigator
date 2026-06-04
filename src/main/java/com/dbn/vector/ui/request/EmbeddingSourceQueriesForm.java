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

import com.dbn.vector.model.request.EmbeddingSourceQueries;
import com.dbn.vector.ui.VectorToolboxFormBase;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.dbn.nls.NlsResources.txt;

public class EmbeddingSourceQueriesForm extends VectorToolboxFormBase {
    private JPanel mainPanel;
    private JPanel queryListPanel;

    private EmbeddingSourceQueriesListForm queryListForm;

    public EmbeddingSourceQueriesForm(@NotNull VectorToolboxFormBase parent) {
        super(parent);
        initTableListForm();
    }

    private void initTableListForm() {
        queryListForm = new EmbeddingSourceQueriesListForm(this);
        queryListPanel.add(queryListForm.getComponent());
    }

    @Override
    protected void initValidation() {
        addValidation(
                queryListForm.getQueriesList(),
                list -> list.getModel().getSize() > 0,
                txt("msg.vector.error.SpecifyAtLeastOneQuery")
        );
    }

    @Override
    public void resetFormChanges() {
        EmbeddingSourceQueries config = getConfig();
        queryListForm.setQueries(config.getElements());
    }

    @Override
    public void applyFormChanges() {
        EmbeddingSourceQueries config = getConfig();
        config.setElements(queryListForm.getQueries());
    }

    public EmbeddingSourceQueries getConfig() {
        return getEmbeddingRequest().getSourceConfig().getSourceQueries();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public int getQueryCount() {
        return queryListForm.getQueriesList().getModel().getSize();
    }
}
