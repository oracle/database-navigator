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

package com.dbn.object.properties;

import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import com.intellij.pom.Navigatable;
import lombok.EqualsAndHashCode;

import javax.swing.Icon;

@EqualsAndHashCode(callSuper = false)
public class ConnectionPresentableProperty extends PresentableProperty{
    private final ConnectionRef connection;

    public ConnectionPresentableProperty(ConnectionHandler connection) {
        this.connection = connection.ref();
    }

    public ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    public String getName() {
        return "Connection";
    }

    @Override
    public String getValue() {
        return getConnection().getName();
    }

    @Override
    public Icon getIcon() {
        return getConnection().getIcon();
    }

    @Override
    public Navigatable getNavigatable() {
        return getConnection().getObjectBundle();
    }
}
