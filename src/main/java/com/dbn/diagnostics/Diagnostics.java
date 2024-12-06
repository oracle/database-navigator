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

package com.dbn.diagnostics;

import com.dbn.common.state.PersistentStateElement;
import com.dbn.common.util.Unsafe;
import com.dbn.diagnostics.data.Activity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;

import static com.dbn.common.options.setting.Settings.booleanAttribute;
import static com.dbn.common.options.setting.Settings.integerAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setBooleanAttribute;
import static com.dbn.common.options.setting.Settings.setIntegerAttribute;
import static com.dbn.common.util.Classes.simpleClassName;
import static com.dbn.common.util.Commons.nvl;

@Slf4j
@UtilityClass
public final class Diagnostics {
    private static final @Getter DeveloperMode developerMode = new DeveloperMode();
    private static final @Getter DebugLogging debugLogging = new DebugLogging();
    private static final @Getter DatabaseLag databaseLag = new DatabaseLag();
    private static final @Getter Miscellaneous miscellaneous = new Miscellaneous();

    public static boolean isDialogSizingReset() {
        return isDeveloperMode() && miscellaneous.dialogSizingReset;
    }

    public static boolean isBulkActionsEnabled() {
        return isDeveloperMode() && miscellaneous.bulkActionsEnabled;
    }

    public static boolean isBackgroundDisposerDisabled() {
        return isDeveloperMode() && miscellaneous.backgroundDisposerDisabled;
    }

    public static boolean isDatabaseAccessDebug() {
        return isDeveloperMode() && debugLogging.databaseAccess;
    }

    public static boolean isDatabaseResourceDebug() {
        return isDeveloperMode() && debugLogging.databaseResource;
    }

    public static boolean isDeveloperMode() {
        return developerMode.isEnabled();
    }

    public static boolean hasEnabledFeatures() {
        return miscellaneous.hasEnabledFeatures() ||
                debugLogging.hasEnabledFeatures() ||
                databaseLag.enabled;
    }

    public static void readState(Element element) {
        if (element == null) return;
        developerMode.readState(element);
        debugLogging.readState(element);
        databaseLag.readState(element);
        miscellaneous.readState(element);
    }

    public static void writeState(Element element) {
        developerMode.writeState(element);
        debugLogging.writeState(element);
        databaseLag.writeState(element);
        miscellaneous.writeState(element);
    }

    @Getter
    @Setter
    public static final class DatabaseLag implements PersistentStateElement {
        private boolean enabled = false;
        private int connectivity = 2000;
        private int querying = 2000;
        private int loading = 500;

        public void readState(Element element) {
            Element databaseLag = element.getChild("database-lag");
            if (databaseLag != null) {
                enabled = booleanAttribute(databaseLag, "enabled", enabled);
                connectivity = integerAttribute(databaseLag, "connectivity", connectivity);
                querying = integerAttribute(databaseLag, "querying", querying);
                loading = integerAttribute(databaseLag, "fetching", loading);
            }

        }

        public void writeState(Element element) {
            Element databaseLag = newElement(element, "database-lag");
            setBooleanAttribute(databaseLag, "enabled", enabled);
            setIntegerAttribute(databaseLag, "connectivity", connectivity);
            setIntegerAttribute(databaseLag, "querying", querying);
            setIntegerAttribute(databaseLag, "fetching", loading);
        }
    }

    @Getter
    @Setter
    public static final class DebugLogging implements PersistentStateElement{
        private boolean failsafeErrors = false;
        private boolean databaseAccess = false;
        private boolean databaseResource = false;

        public boolean hasEnabledFeatures() {
            return failsafeErrors || databaseAccess || databaseResource;
        }

        @Override
        public void readState(Element element) {
            Element debugMode = element.getChild("debug-logging");
            if (debugMode != null) {
                failsafeErrors = booleanAttribute(debugMode, "failsafe-errors", failsafeErrors);
                databaseAccess = booleanAttribute(debugMode, "database-access", databaseAccess);
                databaseResource = booleanAttribute(debugMode, "database-resource", databaseResource);
            }
        }

        @Override
        public void writeState(Element element) {
            Element debugMode = newElement(element, "debug-logging");
            setBooleanAttribute(debugMode, "failsafe-errors", failsafeErrors);
            setBooleanAttribute(debugMode, "database-access", databaseAccess);
            setBooleanAttribute(debugMode, "database-resource", databaseResource);
        }
    }

    @Getter
    @Setter
    public static final class Miscellaneous implements PersistentStateElement{
        private boolean dialogSizingReset = false;
        private boolean bulkActionsEnabled = false;
        private boolean backgroundDisposerDisabled = false;
        private boolean timeoutHandlingDisabled = false;

        public boolean hasEnabledFeatures() {
            return dialogSizingReset ||
                    bulkActionsEnabled ||
                    backgroundDisposerDisabled ||
                    timeoutHandlingDisabled;
        }

        @Override
        public void readState(Element element) {
            Element miscellaneous = element.getChild("miscellaneous");
            if (miscellaneous != null) {
                dialogSizingReset = booleanAttribute(miscellaneous, "dialog-sizing-reset", dialogSizingReset);
                bulkActionsEnabled = booleanAttribute(miscellaneous, "bulk-actions-enabled", bulkActionsEnabled);
                backgroundDisposerDisabled = booleanAttribute(miscellaneous, "background-disposer-disabled", backgroundDisposerDisabled);
                timeoutHandlingDisabled = booleanAttribute(miscellaneous, "timeout-handling-disabled", timeoutHandlingDisabled);
            }
        }

        @Override
        public void writeState(Element element) {
            Element miscellaneous = newElement(element, "miscellaneous");
            setBooleanAttribute(miscellaneous, "dialog-sizing-reset", dialogSizingReset);
            setBooleanAttribute(miscellaneous, "bulk-actions-enabled", bulkActionsEnabled);
            setBooleanAttribute(miscellaneous, "background-disposer-disabled", backgroundDisposerDisabled);
            setBooleanAttribute(miscellaneous, "timeout-handling-disabled", timeoutHandlingDisabled);
        }
    }

    public static int timeoutAdjustment(int timeout) {
        if (!miscellaneous.timeoutHandlingDisabled) return timeout;
        if (!isDeveloperMode()) return timeout;

        return timeout * 100;
    }


    public static void databaseLag(Activity activity) {
        if (!databaseLag.enabled) return;
        if (!isDeveloperMode()) return;

        switch (activity) {
            case CONNECT: lag(databaseLag.connectivity);
            case QUERY: lag(databaseLag.querying);
            case LOAD: lag(databaseLag.loading);
        }

    }

    public static void conditionallyLog(Throwable exception) {
        if (!debugLogging.failsafeErrors) return;
        if (!isDeveloperMode()) return;

        String message = nvl(exception.getMessage(), simpleClassName(exception));
        log.warn("[DIAGNOSTICS] " + message, exception);
    }

    private static void lag(int millis) {
        Unsafe.silent(() -> Thread.sleep(millis));
    }
}
