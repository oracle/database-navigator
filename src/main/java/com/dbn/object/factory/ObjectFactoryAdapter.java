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

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.util.Lists;
import com.dbn.object.DBSchema;
import com.dbn.object.factory.model.DBObjectFactoryInput;
import com.dbn.object.factory.ui.common.DBObjectFactoryInputForm;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.extensions.ExtensionPointName;

import java.sql.SQLException;
import java.util.List;

import static com.dbn.common.util.Unsafe.cast;

public interface ObjectFactoryAdapter<I extends DBObjectFactoryInput, F extends DBObjectFactoryInputForm<I>> {
    ExtensionPointName<ObjectFactoryAdapter> EP = ExtensionPointName.create("com.dbn.objectFactoryAdapter");

    DBObjectType[] getObjectTypes();

    I createInput(DBSchema schema);

    F createInputForm(DBNComponent parent, I input);

    static <A extends ObjectFactoryAdapter> A find(DBObjectType objectType) {
        List<ObjectFactoryAdapter> extensionList = ObjectFactoryAdapter.EP.getExtensionList();
        return cast(Lists.first(extensionList, e -> objectType.isOneOf(e.getObjectTypes())));
    }

    void createObject(I input) throws SQLException;
}
