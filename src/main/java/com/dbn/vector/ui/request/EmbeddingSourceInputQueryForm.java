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

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.vector.model.request.EmbeddingSourceQuery;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

public class EmbeddingSourceInputQueryForm extends DBNFormBase {
    private JPanel mainPanel;

    public EmbeddingSourceInputQueryForm(@NotNull Disposable parent, ConnectionHandler connection, EmbeddingSourceQuery config) {
        super(parent);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
