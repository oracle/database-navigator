/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.common.ui.util;

import com.intellij.openapi.ui.SelectFromListDialog;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
public final class Lists {

    public static void notifyListDataListeners(Object source, Listeners<ListDataListener> listeners, int fromIndex, int toIndex, int eventType) {
        try {
            ListDataEvent event = new ListDataEvent(source, eventType, fromIndex, toIndex);
            listeners.notify(l -> {
                switch (eventType) {
                    case ListDataEvent.INTERVAL_ADDED:   l.intervalAdded(event); break;
                    case ListDataEvent.INTERVAL_REMOVED: l.intervalRemoved(event); break;
                    case ListDataEvent.CONTENTS_CHANGED: l.contentsChanged(event); break;
                }
            });
        } catch (Exception e) {
            conditionallyLog(e);
            log.error("Error notifying actions model listeners", e);
        }
    }

    public static final SelectFromListDialog.ToStringAspect BASIC_TO_STRING_ASPECT = obj -> obj.toString();

}
