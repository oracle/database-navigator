/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.browser.ui;

import com.dbn.browser.model.BrowserTreeNode;
import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.ConnectionId;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

@Getter
public abstract class DatabaseBrowserForm extends DBNFormBase {

    public DatabaseBrowserForm(DBNForm parent) {
        super(parent);
    }

    @Nullable
    public abstract DatabaseBrowserTree getBrowserTree();

    public abstract void selectElement(BrowserTreeNode treeNode, boolean focus, boolean scroll);

    public abstract void selectConnection(ConnectionId connectionId);

    public abstract ConnectionId getSelectedConnection();

    public abstract void rebuildTree();
}
