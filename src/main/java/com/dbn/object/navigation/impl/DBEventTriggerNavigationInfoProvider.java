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

import com.dbn.object.DBEventTrigger;
import com.dbn.object.DBFunction;
import com.dbn.object.common.list.DBObjectNavigationList;
import com.dbn.object.type.DBObjectType;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.dbn.nls.NlsResources.txt;

public class DBEventTriggerNavigationInfoProvider extends DBObjectNavigationInfoProviderBase<DBEventTrigger> {
    public DBEventTriggerNavigationInfoProvider() {
        super(DBObjectType.EVENT_TRIGGER);
    }

    @Override
    public @Nullable List<DBObjectNavigationList<?>> createNavigationTargets(DBEventTrigger trigger) {
        DBFunction triggerFunction = trigger.getTriggerFunction();
        if (triggerFunction == null) return null;

        return List.of(DBObjectNavigationList.create(txt("app.object.label.TriggerFunction"), triggerFunction));
    }
}
