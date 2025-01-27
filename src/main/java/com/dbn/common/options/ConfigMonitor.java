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

package com.dbn.common.options;

import com.dbn.common.dispose.Failsafe;
import com.dbn.common.property.PropertyHolderBase;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
@UtilityClass
public final class ConfigMonitor {
    private static final ThreadLocal<List<SettingsChangeNotifier>> CHANGE_NOTIFIERS = new ThreadLocal<>();
    private static final ThreadLocal<PropertyHolderBase<ConfigActivity>> ACTIVITIES = new ThreadLocal<>();

    public static boolean isCloning() {
        return is(ConfigActivity.CLONING);
    }

    public static boolean is(ConfigActivity activity) {
        PropertyHolderBase<ConfigActivity> propertyHolder = ACTIVITIES.get();
        if (propertyHolder == null) return false; // no property set yet
        return propertyHolder.is(activity);
    }

    public static void set(ConfigActivity activity, boolean value) {
        PropertyHolderBase<ConfigActivity> propertyHolder = ACTIVITIES.get();
        if (propertyHolder == null && !value) return; // nothing to change
        if (propertyHolder == null) {
            propertyHolder = new ConfigurationActivityState();
            ACTIVITIES.set(propertyHolder);
        }
        propertyHolder.set(activity, value);
    }

    public static void registerChangeNotifier(SettingsChangeNotifier notifier) {
        List<SettingsChangeNotifier> notifiers = CHANGE_NOTIFIERS.get();
        if (notifiers == null) {
            notifiers = new ArrayList<>();
            CHANGE_NOTIFIERS.set(notifiers);
        }
        notifiers.add(notifier);
    }

    public static void notifyChanges() {
        List<SettingsChangeNotifier> changeNotifiers = CHANGE_NOTIFIERS.get();
        if (changeNotifiers == null) return;

        CHANGE_NOTIFIERS.remove();
        for (SettingsChangeNotifier changeNotifier : changeNotifiers) {
            try {
                Failsafe.guarded(() -> changeNotifier.notifyChanges());
            } catch (Exception e){
                conditionallyLog(e);
                log.error("Error notifying configuration changes", e);
            }
        }
    }

    private static class ConfigurationActivityState extends PropertyHolderBase.IntStore<ConfigActivity> {
        @Override
        protected ConfigActivity[] properties() {
            return ConfigActivity.VALUES;
        }
    }
}
