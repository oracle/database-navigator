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

import com.dbn.object.DBSchema;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DBMethodFactoryInput extends DBSchemaObjectFactoryInput {
    private DBObjectFactoryInputList<DBArgumentFactoryInput> arguments = new DBObjectFactoryInputList<>(this);
    private DBArgumentFactoryInput returnArgument;

    public DBMethodFactoryInput(DBSchema schema, DBObjectType methodType) {
        super(schema, methodType);
        if (methodType == DBObjectType.FUNCTION) {
            returnArgument = new DBArgumentFactoryInput(this, 0, "return", null, false, true);
        }

        // add first empty argument
        arguments.add(new DBArgumentFactoryInput(this, 0));
    }

    public boolean isFunction() {
        return returnArgument != null;
    }
}
