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

import com.dbn.common.util.Naming;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

@Getter
@Setter
public class CustomNamingProvider implements WrapperNamingProvider {
    private String javaWrapperName;
    private String sqlWrapperName;
    private Map<String, String> sqlPackageMethodMap;
    private Map<String, String> sqlTypesMap;

    @Override
    public String getJavaWrapperName(DBJavaClass javaClass) {
        return javaWrapperName;
    }

    @Override
    public String getJavaWrapperName(DBJavaMethod javaMethod) {
        return javaWrapperName;
    }

    @Override
    public String getSqlWrapperName(DBJavaClass javaClass) {
        return sqlWrapperName;
    }

    @Override
    public String getSqlWrapperName(DBJavaMethod javaMethod) {
        return sqlWrapperName;
    }

    @Override
    public String getSqlTypeName(DBJavaClass javaClass, int arrayDepth) {
        return "";
    }

    @Override
    public String getSqlTypeName(String javaClassName, int arrayDepth) {
        if(sqlTypesMap == null) return "" ;
        String typeName = toSqlTypeName(javaClassName, "TYPE");
        if (arrayDepth > 0) typeName += "_" + arrayDepth;
        return sqlTypesMap.get(typeName);
    }

    @Override
    public String getSqlMethodName(DBJavaMethod javaMethod) {
        if(sqlPackageMethodMap == null) return "" ;
        return sqlPackageMethodMap.get(Naming.toUpperSnakeCase(javaMethod.getSimpleName()));
    }

    private @NotNull String toSqlTypeName(String className, String qualifier) {
        return "OJVM_" + qualifier + "_" + className.replace(".", "_").replace("$", "_").toUpperCase();
    }
}
