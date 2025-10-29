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

package com.dbn.vector.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.ui.util.ComboBoxes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.common.DBObject;
import com.intellij.openapi.Disposable;
import org.jetbrains.annotations.Nullable;

public abstract class VectorToolboxFormBase extends DBNFormBase {
    private final ConnectionRef connection;

    public VectorToolboxFormBase(@Nullable Disposable parent, ConnectionHandler connection) {
        super(parent);
        this.connection = connection.ref();
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    protected static String getSelectedObjectName(DBNComboBox<? extends DBObject> comboBox) {
        DBObject selection = ComboBoxes.getSelection(comboBox);
        return selection == null ? "" : selection.getName();
    }
}
