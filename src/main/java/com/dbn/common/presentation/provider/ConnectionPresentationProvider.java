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

package com.dbn.common.presentation.provider;


import com.dbn.connection.ConnectionHandler;

import javax.swing.Icon;

import static com.dbn.nls.NlsResources.txt;

public class ConnectionPresentationProvider extends PresentationProviderBase<ConnectionHandler> {
    public ConnectionPresentationProvider() {
        super(ConnectionHandler.class);
    }

    @Override
    public String getName(ConnectionHandler object) {
        return object.getName();
    }

    @Override
    public String getTypeName(ConnectionHandler object) {
        return txt("app.connection.text.DatabaseConnection");
    }

    @Override
    public String getDescription(ConnectionHandler object) {
        return object.getDescription();
    }

    @Override
    public Icon getIcon(ConnectionHandler object) {
        return object.getIcon();
    }
}
