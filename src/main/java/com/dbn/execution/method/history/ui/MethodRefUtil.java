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

package com.dbn.execution.method.history.ui;

import com.dbn.object.DBMethod;
import com.dbn.object.DBProgram;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;

@Deprecated
public class MethodRefUtil {
    public static DBProgram getProgram(DBObjectRef<DBMethod> methodRef) {
        return (DBProgram) methodRef.getParentObject(DBObjectType.PROGRAM);
    }

    public static String getProgramName(DBObjectRef<DBMethod> methodRef) {
        DBObjectRef programRef = methodRef.getParentRef(DBObjectType.PROGRAM);
        return programRef == null ? null : programRef.getObjectName();
    }

    public static DBObjectType getProgramObjectType(DBObjectRef<DBMethod> methodRef) {
        DBObjectRef programRef = methodRef.getParentRef(DBObjectType.PROGRAM);
        return programRef == null ? null : programRef.getObjectType();
    }
}
