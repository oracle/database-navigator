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

package com.dbn.execution.java.wrapper.naming;

import com.dbn.common.Pair;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;

import java.util.HashMap;
import java.util.Map;

import static com.dbn.common.exception.Exceptions.unsupported;

public class TransientWrapperNamingProvider implements WrapperNamingProvider {
    public static final String NAME_PREFIX = "DBN_OJVM_";
    private final Map<Pair<String, Integer>, String> typeNames = new HashMap<>();

    @Override
    public String getJavaWrapperName(DBJavaClass javaClass) {
        return NAME_PREFIX + "JAVA_WRAPPER";
    }

    @Override
    public String getJavaWrapperName(DBJavaMethod javaMethod) {
        return NAME_PREFIX + "JAVA_WRAPPER";
    }

    @Override
    public String getSqlWrapperName(DBJavaClass javaClass) {
        return unsupported(); // transient wrappers are expected to revolve around methods only
    }

    @Override
    public String getSqlWrapperName(DBJavaMethod javaMethod) {
        return javaMethod.isReturningVoid() ?
                NAME_PREFIX + "SQL_PROCEDURE_WRAPPER" :
                NAME_PREFIX + "SQL_FUNCTION_WRAPPER";
    }

    @Override
    public String getSqlTypeName(DBJavaClass javaClass, int arrayDepth) {
        var key = Pair.of(javaClass.getName(), arrayDepth);
        int size = typeNames.size();

        return typeNames.computeIfAbsent(key, object -> NAME_PREFIX + "TYPE_" + size);
    }

    @Override
    public String getSqlMethodName(DBJavaMethod javaMethod) {
        return unsupported(); // transient wrappers are expected to use getSqlWrapperName(javaMethod)
    }
}
