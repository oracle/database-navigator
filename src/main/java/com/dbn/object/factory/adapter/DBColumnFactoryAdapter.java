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
import com.dbn.object.factory.model.DBObjectSpec;
import com.dbn.object.factory.ui.DBColumnFactoryInputForm;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.Nls;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.nls.NlsResources.txt;
import static com.dbn.object.factory.model.DBObjectAttributeType.DATA_TYPE;
import static com.dbn.object.type.DBObjectType.COLUMN;

public class DBColumnFactoryAdapter implements ObjectFactoryAdapter {

    @Override
    public DBObjectType getObjectType() {
        return COLUMN;
    }

    public DBObjectSpec createInput(DBSchema schema) {
        //return new DBColumnFactoryInput(schema);
        return null; // TODO
    }

    public DBColumnFactoryInputForm createInputForm(DBNComponent parent, DBObjectSpec columnSpec) {
        return new DBColumnFactoryInputForm(parent, columnSpec);
    }

    @Override
    public void validateInput(DBObjectSpec columnSpec, List<@Nls String> errors) {
        String objectName = columnSpec.getObjectName();
        int inputIndex = columnSpec.getIndex();
        if (objectName.isEmpty()) {
            errors.add(txt("msg.objects.error.ColumnNameNotSpecifiedAtIndex", inputIndex));

        } else if (!Strings.isWord(objectName)) {
            errors.add(txt("msg.objects.error.ColumnNameInvalidAtIndex", inputIndex, objectName));
        }

        String dataType = DATA_TYPE.of(columnSpec);
        if (Strings.isEmptyOrSpaces(dataType)){
            if (!objectName.isEmpty()) {
                errors.add(txt("msg.objects.error.ColumnDataTypeMissingForName", objectName));
            } else {
                errors.add(txt("msg.objects.error.ColumnDataTypeMissingAtIndex", inputIndex));
            }
        }
    }

    @Override
    public void createObject(DBObjectSpec columnSpec) throws SQLException {
        // child object - created as part of the parent
    }
}
