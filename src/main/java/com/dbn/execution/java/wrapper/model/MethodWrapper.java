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

package com.dbn.execution.java.wrapper.model;

import com.dbn.execution.java.wrapper.WrapperModel;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBMethod;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.common.dispose.Failsafe.nd;

@Getter
@Setter
public class MethodWrapper extends EntityWrapper {
    private final DBObjectRef<DBJavaMethod> javaMethod;
    private final DBObjectRef<DBMethod> sqlMethod;

    // method signatures of wrapper java methods may be same even though original java methods with same name have different signatures
    private final String surrogateJavaMethodName;

    private List<ParameterWrapper> parameters = new ArrayList<>();
    private ParameterWrapper returnParameter;

    public MethodWrapper(WrapperModel model, DBJavaMethod javaMethod) {
        super(model);
        this.javaMethod = DBObjectRef.of(javaMethod);
        this.sqlMethod = initSqlMethod(javaMethod);
        surrogateJavaMethodName = javaMethod.getName().replace("#", "_");
    }

    private DBObjectRef<DBMethod> initSqlMethod(DBJavaMethod javaMethod) {
        String sqlMethodName = getNamingProvider().getSqlMethodName(javaMethod);
        DBObjectType sqlMethodType = javaMethod.isReturningVoid() ?
                DBObjectType.PROCEDURE :
                DBObjectType.FUNCTION;

        DBObjectRef<?> sqlPackage = getModel().getSqlWrapperPackage();
        DBObjectRef<?> sqlMethodParent = sqlPackage == null ? nd(javaMethod.getSchema()).ref() : sqlPackage;
        return new DBObjectRef<>(sqlMethodParent, sqlMethodType, sqlMethodName);
    }

    public String getJavaMethodName() {
        return getJavaMethod().getSimpleName();
    }

    public String getSqlMethodName() {
        return sqlMethod.getObjectName();
    }

    /**
     * The original method of java class being wrapped
     */
    public DBJavaMethod getJavaMethod() {
        return DBObjectRef.ensure(javaMethod);
    }

    public void addParameter(ParameterWrapper parameterWrapper) {
        parameters.add(parameterWrapper);
    }
}
