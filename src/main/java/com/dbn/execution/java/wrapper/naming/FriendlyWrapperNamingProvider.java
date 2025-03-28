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

import com.dbn.common.exception.Exceptions;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.util.Naming.toUpperSnakeCase;

public class FriendlyWrapperNamingProvider implements WrapperNamingProvider {

    @Override
    public String getJavaWrapperName(DBJavaClass javaClass) {
        return toSqlTypeName(javaClass, "CLASS");  // e.g. COM_ABC_SHAPE_CLASS
    }

    @Override
    public String getJavaWrapperName(DBJavaMethod javaMethod) {
        DBJavaClass javaClass = javaMethod.getOwnerClass();
        return toSqlTypeName(javaClass, "METHOD") + "_" + toUpperSnakeCase(javaMethod.getSimpleName());
    }

    @Override
    public String getSqlWrapperName(DBJavaClass javaClass) {
        return toSqlTypeName(javaClass, "PACKAGE"); // e.g. OJVM_PACKAGE_COM_ABC_SHAPE
    }

    @Override
    public String getSqlWrapperName(DBJavaMethod javaMethod) {
        DBJavaClass javaClass = javaMethod.getOwnerClass();
        String qualifier = javaMethod.isReturningVoid() ? "PROCEDURE" : "FUNCTION";
        return toSqlTypeName(javaClass, qualifier) + "_" + toUpperSnakeCase(javaMethod.getSimpleName());
    }

    @Override
    public String getSqlTypeName(DBJavaClass javaClass, int arrayDepth) {
        String typeName = toSqlTypeName(javaClass, "TYPE");
        if (arrayDepth > 0) typeName += "_" + arrayDepth;
        return typeName; // e.g. OJVM_TYPE_COM_ABC_SHAPE
    }

    @Override
    public String getSqlTypeName(String javaClassName, int arrayDepth) {
        String typeName = toSqlTypeName(javaClassName, "TYPE");
        if (arrayDepth > 0) typeName += "_" + arrayDepth;
        return typeName; // e.g. OJVM_TYPE_JAVA_LANG_STRING_1
    }

    @Override
    public String getSqlMethodName(DBJavaMethod javaMethod) {
        Exceptions.unsupported();
        return null;
    }


    private static String toSqlTypeName(DBJavaClass javaClass, String qualifier) {
        String className = javaClass.getCanonicalName();
        return toSqlTypeName(className, qualifier);
    }

    private static @NotNull String toSqlTypeName(String className, String qualifier) {
        return "OJVM_" + qualifier + "_" + className.replace(".", "_").replace("$", "_").toUpperCase();
    }
}
