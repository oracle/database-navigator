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

package com.dbn.object.factory.ui;

import com.dbn.common.ui.component.DBNComponent;
import com.dbn.data.type.ui.DataTypeEditor;
import com.dbn.object.DBSchema;
import com.dbn.object.factory.ArgumentFactoryInput;
import com.dbn.object.factory.MethodFactoryInput;
import com.dbn.object.factory.ObjectFactoryInput;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.NotNull;

public class FunctionFactoryInputForm extends MethodFactoryInputForm {

    public FunctionFactoryInputForm(@NotNull DBNComponent parent, DBSchema schema, DBObjectType objectType, int index) {
        super(parent, schema, objectType, index);
    }

    @Override
    public MethodFactoryInput createFactoryInput(ObjectFactoryInput parent) {
        MethodFactoryInput methodFactoryInput = super.createFactoryInput(parent);

        DataTypeEditor returnTypeEditor = (DataTypeEditor) returnArgumentDataTypeEditor;

        ArgumentFactoryInput returnArgument = new ArgumentFactoryInput(
                methodFactoryInput, 0, "return",
                returnTypeEditor.getDataTypeRepresentation(),
                false, true);

        methodFactoryInput.setReturnArgument(returnArgument);
        return methodFactoryInput;
    }

    @Override
    public boolean hasReturnArgument() {
        return true;
    }
}
