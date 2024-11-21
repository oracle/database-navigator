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

package com.dbn.object.factory.ui.common;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;

@Getter
@Setter
public abstract class ObjectFactoryInputForm<T extends ObjectFactoryInput> extends DBNFormBase {
    private int index;
    private final ConnectionRef connection;
    private final DBObjectType objectType;

    protected ObjectFactoryInputForm(@NotNull DBNComponent parent, @NotNull ConnectionHandler connection, DBObjectType objectType, int index) {
        super(parent);
        this.connection = connection.ref();
        this.objectType = objectType;
        this.index = index;
    }

    @NotNull
    @Override
    public abstract JPanel getMainComponent();

    @NotNull
    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    public abstract T createFactoryInput(ObjectFactoryInput parent);

    public abstract void focus();
}
