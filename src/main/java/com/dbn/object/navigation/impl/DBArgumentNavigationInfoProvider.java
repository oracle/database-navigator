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

import com.dbn.data.type.DBDataType;
import com.dbn.object.DBArgument;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.type.DBObjectType;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class DBArgumentNavigationInfoProvider extends DBObjectNavigationInfoProviderBase<DBArgument> {
    public DBArgumentNavigationInfoProvider() {
        super(DBObjectType.ARGUMENT);
    }

    @Override
    public List<DBObjectNavigationList<?>> createNavigationTargets(DBArgument argument) {
        DBDataType dataType = argument.getDataType();
        if (!dataType.isDeclared()) return null;

        return List.of(DBObjectNavigationList.create(txt("app.objects.navigation.Type"), dataType.getDeclaredType()));
    }
}
