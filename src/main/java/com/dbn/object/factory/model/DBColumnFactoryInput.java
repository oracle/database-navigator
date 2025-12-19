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
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NonNls;

import java.util.List;


@Getter
@Setter
public class DBColumnFactoryInput extends DBObjectFactoryInput {
    private String dataType;
    private boolean nonNull;
    private boolean primaryKey;

    public DBColumnFactoryInput(DBObjectFactoryInput parent, int index) {
        this(parent, index, null, null, false, false);
    }

    public DBColumnFactoryInput(DBObjectFactoryInput parent, int index, @NonNls String objectName, @NonNls String dataType, boolean nonNull, boolean primaryKey) {
        super(parent, objectName, DBObjectType.COLUMN, index);
        this.dataType = dataType == null ? "" : dataType.trim();
        this.nonNull = nonNull;
        this.primaryKey = primaryKey;
    }

    @Override
    public void validate(List<String> errors) {
        String objectName = getObjectName();
        if (objectName.isEmpty()) {
            errors.add("column name is not specified at index " + getIndex());

        } else if (!Strings.isWord(objectName)) {
            errors.add("invalid column name specified at index " + getIndex() + ": \"" + objectName + "\"");
        }

        if (dataType.isEmpty()){
            if (!objectName.isEmpty()) {
                errors.add("missing data type for column \"" + objectName + "\"");
            } else {
                errors.add("missing data type for column at index " + getIndex());
            }
        }
    }
}
