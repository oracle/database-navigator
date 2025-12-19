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

import com.dbn.common.util.Strings;
import com.dbn.object.DBSchema;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    @Override
    public void validate(List<String> errors) {
        String objectName = getObjectName();
        if (objectName.isEmpty()) {
            String hint = getParent() == null ? "" : " at index " + getIndex();
            errors.add(getObjectType().getName() + " name is not specified" + hint);
            
        } else if (!Strings.isWord(objectName)) {
            errors.add("invalid " + getObjectType().getName() + " name specified" + ": \"" + objectName + "\"");
        }


        if (returnArgument != null) {
            if (returnArgument.getDataType().isEmpty())
                errors.add("missing data type for return argument");
        }

        Set<String> argumentNames = new HashSet<>();
        for (DBArgumentFactoryInput argument : getArguments()) {
            argument.validate(errors);
            String argumentName = argument.getObjectName();
            if (Strings.isEmptyOrSpaces(argumentName)) continue; // already covered by field validator

            if (argumentNames.contains(argumentName)) {
                String hint = getParent() == null ? "" : " for " + getObjectType().getName() + " \"" + objectName + "\"";
                errors.add("duplicate argument name \"" + argumentName + "\"" + hint);
            }
            argumentNames.add(argumentName);
        }
    }
}
