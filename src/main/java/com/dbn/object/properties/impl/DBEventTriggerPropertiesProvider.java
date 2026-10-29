/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.object.properties.impl;

import com.dbn.object.DBEventTrigger;
import com.dbn.object.DBFunction;
import com.dbn.object.properties.DBObjectProperty;
import com.dbn.object.properties.DBObjectPresentableProperty;
import com.dbn.object.properties.SimplePresentableProperty;
import com.dbn.object.type.DBObjectType;
import com.dbn.object.type.DBTriggerEvent;
import com.dbn.object.type.DBTriggerTarget;
import com.dbn.object.type.DBTriggerType;

import java.util.List;

import static com.dbn.common.util.Strings.cachedUpperCase;
import static com.dbn.nls.NlsResources.txt;

public class DBEventTriggerPropertiesProvider extends DBGenericObjectPropertiesProvider<DBEventTrigger> {
    public DBEventTriggerPropertiesProvider() {
        super(DBObjectType.EVENT_TRIGGER);
    }

    @Override
    public List<DBObjectProperty> getProperties(DBEventTrigger trigger) {
        List<DBObjectProperty> properties = super.getProperties(trigger);
        DBTriggerEvent[] triggerEvents = trigger.getTriggerEvents();
        StringBuilder events = new StringBuilder();
        for (DBTriggerEvent event : triggerEvents) {
            if (events.length() > 0) events.append(' ').append(txt("app.objects.propertyValue.Or")).append(' ');
            events.append(cachedUpperCase(event.getName()));
        }

        DBFunction triggerFunction = trigger.getTriggerFunction();
        if (triggerFunction != null) {
            properties.add(0, new DBObjectPresentableProperty(
                    txt("app.object.label.TriggerFunction"), triggerFunction, true));
        }
        properties.add(0, new SimplePresentableProperty(txt("app.objects.property.TriggerTarget"), cachedUpperCase(trigger.getTriggerTarget().getName())));
        properties.add(0, new SimplePresentableProperty(txt("app.objects.property.TriggerEvent"), events.toString()));
        if (trigger.getTriggerType() != DBTriggerType.UNKNOWN) {
            properties.add(0, new SimplePresentableProperty(txt("app.objects.property.TriggerType"), cachedUpperCase(trigger.getTriggerType().getName())));
        }
        if (trigger.getTriggerTarget() == DBTriggerTarget.DATASET) {
            properties.add(0, new SimplePresentableProperty(txt("app.objects.property.ForEachRow"), trigger.isForEachRow()));
        }
        return properties;
    }
}
