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

package com.dbn.object;

import com.dbn.language.common.DBLanguage;
import com.dbn.object.common.DBSchemaObject;
import com.dbn.object.type.DBMethodType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface DBMethod extends DBSchemaObject, DBOrderedObject {
    List<DBArgument> getArguments();
    DBArgument getArgument(String name);
    DBArgument getReturnArgument();
    DBProgram getProgram();
    DBMethodType getMethodType();

    short getPosition();

    boolean isProgramMethod();
    boolean isDeterministic();
    boolean hasDeclaredArguments();
    @NotNull
    DBLanguage getLanguage();
}
