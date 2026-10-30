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

package com.dbn.telemetry;

import com.dbn.common.action.ComboBoxAction;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Shared invocation filtering and identifier normalization for action telemetry. */
@NonNls
@UtilityClass
public class ActionTelemetry {
    private static final String[] SUFFIXES = {
            "IntentionAction", "ComboBoxAction", "DropdownAction", "ToggleAction", "Action"};

    public static boolean isTelemetryEnabled(@NotNull AnAction action, @NotNull AnActionEvent event) {
        if (action instanceof ComboBoxAction comboBox) return comboBox.isTelemetryEnabled();
        if (action instanceof com.intellij.openapi.actionSystem.ex.ComboBoxAction) return false;
        return !(action instanceof ActionGroup);
    }

    /** Keeps registered IDs and class namespaces, but merges presentation-specific suffixes. */
    public static @Nullable String resolveActionId(@NotNull AnAction action) {
        String registeredId = ActionManager.getInstance().getId(action);
        if (registeredId != null) {
            return registeredId.startsWith("DBN.") ? normalizeId(registeredId) : null;
        }

        String className = action.getClass().getCanonicalName();
        if (className == null || !className.startsWith("com.dbn.")) return null;
        return "DBN.Action." + normalizeId(className.substring("com.dbn.".length()));
    }

    private static @NotNull String normalizeId(@NotNull String id) {
        int nameStart = id.lastIndexOf('.') + 1;
        for (String suffix : SUFFIXES) {
            if (id.endsWith(suffix) && id.length() - nameStart > suffix.length()) {
                return id.substring(0, id.length() - suffix.length());
            }
        }
        return id;
    }
}
