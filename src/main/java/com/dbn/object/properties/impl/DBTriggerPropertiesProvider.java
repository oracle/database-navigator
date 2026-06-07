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

package com.dbn.object.properties.impl;

import com.dbn.object.DBTrigger;
import com.dbn.object.properties.DBObjectProperty;
import com.dbn.object.properties.SimplePresentableProperty;
import com.dbn.object.type.DBObjectType;
import com.dbn.object.type.DBTriggerEvent;

import java.util.List;

import static com.dbn.common.util.Strings.cachedLowerCase;
import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.nls.NlsResources.txt;

public class DBTriggerPropertiesProvider extends DBGenericObjectPropertiesProvider<DBTrigger> {
    public DBTriggerPropertiesProvider() {
        super(DBObjectType.TRIGGER);
    }

    @Override
    public List<DBObjectProperty> getProperties(DBTrigger trigger) {
        List<DBObjectProperty> properties = super.getProperties(trigger);
        StringBuilder events = new StringBuilder(cachedLowerCase(trigger.getTriggerType().getName()));
        events.append(" ");
        DBTriggerEvent[] triggerEvents = trigger.getTriggerEvents();
        for (DBTriggerEvent triggeringEvent : triggerEvents) {
            if (triggeringEvent != triggerEvents[0]) events.append(' ').append(txt("app.objects.propertyValue.Or")).append(' ');
            events.append(cachedUpperCase(triggeringEvent.getName()));
        }

        properties.add(0, new SimplePresentableProperty(txt("app.objects.property.TriggerEvent"), events.toString()));
        return properties;
    }
}
