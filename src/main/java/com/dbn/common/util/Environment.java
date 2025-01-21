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

package com.dbn.common.util;

import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationInfo;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@UtilityClass
public class Environment {
    private static final Map<String, String> variables = new ConcurrentHashMap<>();
    private static final boolean HAS_JAVA_SUPPORT = Language.findLanguageByID("JAVA") != null;

    public static boolean isIdeNewerThan(String targetVersion) {
        String currentVersion = ApplicationInfo.getInstance().getFullVersion();
        return isVersionGreaterThan(currentVersion, targetVersion);
    }

    public static boolean hasJavaSupport() {
        return HAS_JAVA_SUPPORT;
    }

    private static boolean isVersionGreaterThan(String currentVersion, String targetVersion) {
        String[] currentParts = currentVersion.split("\\.");
        String[] targetParts = targetVersion.split("\\.");

        int currentMajor = Integer.parseInt(currentParts[0]);
        int targetMajor = Integer.parseInt(targetParts[0]);

        if (currentMajor > targetMajor) return true;
        if (currentMajor < targetMajor) return false;

        int currentMinor = Integer.parseInt(currentParts[1]);
        int targetMinor = Integer.parseInt(targetParts[1]);

        return currentMinor > targetMinor;
    }

    @NonNls
    public static String getVariable(@NonNls String key) {
        return variables.computeIfAbsent(key, k -> loadVariable(k));
    }

    private static String loadVariable(String name) {
        String property = System.getProperty(name);
        if (Strings.isNotEmptyOrSpaces(property)) return property.trim();

        Map<String, String> environmentVariables = System.getenv();
        for (String variableName : environmentVariables.keySet()) {
            if (variableName.equalsIgnoreCase(name)) {
                return environmentVariables.get(variableName);
            }
        }

        return null;
    }
}
