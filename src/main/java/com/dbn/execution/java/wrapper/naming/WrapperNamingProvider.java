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

import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.common.DBObject;
import org.jetbrains.annotations.NonNls;

import static com.dbn.common.exception.Exceptions.unsupported;

@NonNls
public interface WrapperNamingProvider {
    default String getJavaWrapperName(DBObject sourceObject) {
        if (sourceObject instanceof DBJavaClass javaClass) {
            return getJavaWrapperName(javaClass);
        }

        if (sourceObject instanceof DBJavaMethod javaMethod) {
            return getJavaWrapperName(javaMethod);
        }

        return unsupported();
    }

    default String getSqlWrapperName(DBObject sourceObject) {
        if (sourceObject instanceof DBJavaClass javaClass) {
            return getSqlWrapperName(javaClass);
        }

        if (sourceObject instanceof DBJavaMethod javaMethod) {
            return getSqlWrapperName(javaMethod);
        }

        return unsupported();
    }

    String getJavaWrapperName(DBJavaClass javaClass);
    String getJavaWrapperName(DBJavaMethod javaMethod);

    String getSqlWrapperName(DBJavaClass javaClass);
    String getSqlWrapperName(DBJavaMethod javaMethod);

    String getSqlTypeName(DBJavaClass javaClass, int arrayDepth);
    String getSqlTypeName(String javaClassName, int arrayDepth);
    String getSqlMethodName(DBJavaMethod javaMethod);


}
