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

package com.dbn.object.factory.ui;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.connection.ConnectionHandler;
import com.dbn.object.factory.ArgumentFactoryInput;
import com.dbn.object.factory.ui.common.ObjectFactoryInputForm;
import com.dbn.object.factory.ui.common.ObjectListForm;
import com.dbn.object.type.DBObjectType;

public class ArgumentFactoryInputListForm extends ObjectListForm<ArgumentFactoryInput> {
    private final boolean enforceInArguments;
    public ArgumentFactoryInputListForm(DBNComponent parent, ConnectionHandler connection, boolean enforceInArguments) {
        super(parent, connection);
        this.enforceInArguments = enforceInArguments;
    }

    @Override
    public ObjectFactoryInputForm<ArgumentFactoryInput> createObjectDetailsPanel(int index) {
        return new ArgumentFactoryInputForm(this, getConnection(), enforceInArguments, index);
    }

    @Override
    public DBObjectType getObjectType() {
        return DBObjectType.ARGUMENT;
    }
}
