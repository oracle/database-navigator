/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.object.properties.impl;

import com.dbn.data.type.DBDataType;
import com.dbn.object.DBArgument;
import com.dbn.object.properties.DBDataTypePresentableProperty;
import com.dbn.object.properties.DBObjectProperty;
import com.dbn.object.properties.SimplePresentableProperty;
import com.dbn.object.type.DBObjectType;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class DBArgumentPropertiesProvider extends DBGenericObjectPropertiesProvider<DBArgument> {
    public DBArgumentPropertiesProvider() {
        super(DBObjectType.ARGUMENT);
    }

    @Override
    public List<DBObjectProperty> getProperties(DBArgument argument) {
        List<DBObjectProperty> properties = super.getProperties(argument);

        DBDataType dataType = argument.getDataType();
        properties.add(0, new DBDataTypePresentableProperty(dataType));
        properties.add(0, new SimplePresentableProperty(txt("app.objects.property.ArgumentType"), getArgumentType(argument)));
        return properties;
    }

    private static String getArgumentType(DBArgument argument) {
        return argument.isInput() && argument.isOutput() ? txt("app.objects.propertyValue.ArgumentInOut") :
                argument.isInput() ? txt("app.objects.propertyValue.ArgumentIn") :
                txt("app.objects.propertyValue.ArgumentOut");
    }
}
