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

package com.dbn.object.navigation.impl;

import com.dbn.common.util.Java;
import com.dbn.object.DBJavaClass;
import com.dbn.object.DBJavaMethod;
import com.dbn.object.DBJavaParameter;
import com.dbn.object.common.list.DBObjectList;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.common.list.ObjectListProvider;
import com.dbn.object.lookup.DBObjectRef;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class DBJavaMethodNavigationInfoProvider extends DBObjectNavigationInfoProviderBase<DBJavaMethod> {
    public DBJavaMethodNavigationInfoProvider() {
        super(DBObjectType.JAVA_METHOD);
    }

    @Override
    public List<DBObjectNavigationList<?>> createNavigationTargets(DBJavaMethod method) {
        List<DBObjectNavigationList<?>> navigationLists = new LinkedList<>();
        DBObjectList<DBJavaParameter> parameterList = initParameterList(method);
        if (parameterList != null) {
            if (parameterList.isLoaded()) {
                List<DBJavaParameter> parameters = method.getParameters();
                if (!parameters.isEmpty()) navigationLists.add(DBObjectNavigationList.create("Parameters", parameters));
            } else {
                ObjectListProvider<DBJavaParameter> provider = () -> method.getParameters();
                navigationLists.add(DBObjectNavigationList.create("Parameters", provider)); // lazy
            }
        }

        DBObjectRef<DBJavaClass> returnClassRef = method.getReturnClassRef();
        if (returnClassRef != null) {
            String returnClassName = returnClassRef.getObjectName();
            if (!Java.isScalar(returnClassName) &&
                    !Java.isVoid(returnClassName)) {
                if (returnClassRef.isLoaded()) {
                    navigationLists.add(DBObjectNavigationList.create("Return Type", method.getReturnClass()));
                } else {
                    ObjectListProvider<DBJavaClass> provider = () -> {
                        DBJavaClass returnClass = method.getReturnClass();
                        return returnClass == null ? Collections.emptyList() : List.of(returnClass);
                    };
                    navigationLists.add(DBObjectNavigationList.create("Return Type", provider));
                }
            }
        }

        return navigationLists;
    }

    @Nullable
    private DBObjectList<DBJavaParameter> initParameterList(DBJavaMethod method) {
        DBObjectList<DBJavaParameter> parameterList = method.getChildObjectList(DBObjectType.JAVA_PARAMETER);
        if (parameterList == null) return null;
        if (parameterList.isLoaded()) return parameterList;

        if (!parameterList.isLoading()) parameterList.loadInBackground();
        return null;
    }
}
