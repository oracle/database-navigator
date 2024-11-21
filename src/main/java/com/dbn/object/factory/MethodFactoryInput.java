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

package com.dbn.object.factory;

import com.dbn.common.util.Strings;
import com.dbn.object.DBSchema;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class MethodFactoryInput extends ObjectFactoryInput{
    private List<ArgumentFactoryInput> arguments = new ArrayList<>();
    private ArgumentFactoryInput returnArgument;
    private final DBObjectRef<DBSchema> schema;

    public MethodFactoryInput(DBSchema schema, String objectName, DBObjectType methodType, int index) {
        super(objectName, methodType, null, index);
        this.schema = DBObjectRef.of(schema);
    }

    public DBSchema getSchema() {
        return DBObjectRef.get(schema);
    }

    public boolean isFunction() {
        return returnArgument != null;
    }

    @Override
    public void validate(List<String> errors) {
        String objectName = getObjectName();
        if (objectName.length() == 0) {
            String hint = getParent() == null ? "" : " at index " + getIndex();
            errors.add(getObjectType().getName() + " name is not specified" + hint);
            
        } else if (!Strings.isWord(objectName)) {
            errors.add("invalid " + getObjectType().getName() +" name specified" + ": \"" + objectName + "\"");
        }


        if (returnArgument != null) {
            if (returnArgument.getDataType().length() == 0)
                errors.add("missing data type for return argument");
        }

        Set<String> argumentNames = new HashSet<>();
        for (ArgumentFactoryInput argument : arguments) {
            argument.validate(errors);
            String argumentName = argument.getObjectName();
            if (argumentNames.contains(argumentName)) {
                String hint = getParent() == null ? "" : " for " + getObjectType().getName() + " \"" + objectName + "\"";
                errors.add("duplicate argument name \"" + argumentName + "\"" + hint);
            }
            argumentNames.add(argumentName);
        }
    }
}
