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

package com.dbn.object.factory.model;

import com.dbn.connection.ConnectionHandler;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
public class DBObjectFactoryInputList<T extends DBObjectFactoryInput> extends ArrayList<T> {
    private final DBObjectFactoryInput parent;
    private boolean readonly;

    public DBObjectFactoryInputList(DBObjectFactoryInput parent) {
        this.parent = parent;
    }

    public ConnectionHandler getConnection() {
        return parent.getConnection();
    }
}
