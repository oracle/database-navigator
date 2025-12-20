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

package com.dbn.object.factory.adapter;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.util.Strings;
import com.dbn.object.DBSchema;
import com.dbn.object.factory.ObjectFactoryAdapter;
import com.dbn.object.factory.model.DBArgumentFactoryInput;
import com.dbn.object.factory.ui.DBArgumentFactoryInputForm;
import com.dbn.object.type.DBObjectType;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.object.type.DBObjectType.ARGUMENT;

public class DBArgumentFactoryAdapter implements ObjectFactoryAdapter<DBArgumentFactoryInput, DBArgumentFactoryInputForm> {

    @Override
    public DBObjectType getObjectType() {
        return ARGUMENT;
    }

    public DBArgumentFactoryInput createInput(DBSchema schema) {
        //return new DBArgumentFactoryInput(schema);
        return null; // TODO
    }

    public DBArgumentFactoryInputForm createInputForm(DBNComponent parent, DBArgumentFactoryInput input) {
        return new DBArgumentFactoryInputForm(parent, input);
    }

    @Override
    public void validateInput(DBArgumentFactoryInput input, List<String> errors) {
        String objectName = input.getObjectName();
        int inputIndex = input.getIndex();
        if (objectName.isEmpty()) {
            errors.add("argument name is not specified at index " + inputIndex);

        } else if (!Strings.isWord(objectName)) {
            errors.add("invalid argument name specified at index " + inputIndex + ": \"" + objectName + "\"");
        }

        if (Strings.isEmptyOrSpaces(input.getDataType())){
            if (objectName.length() > 0) {
                errors.add("missing data type for argument \"" + objectName + "\"");
            } else {
                errors.add("missing data type for argument at index " + inputIndex);
            }
        }
    }

    @Override
    public void createObject(DBArgumentFactoryInput input) throws SQLException {
        // child object - created as part of the parent
    }
}
