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

package com.dbn.common.util;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

import java.util.Set;

@UtilityClass
public class Identifiers {

    /**
     * Sanitizes a name for use as an unquoted identifier and adds it to {@code taken}.
     * Name collisions are checked without regard to case.
     */
    public static String generateSafeIdentifier(@NonNls String name, Set<String> taken) {
        String identifier = name
                .replaceAll("[^A-Za-z0-9]", "_")
                .replaceAll("_+", "_");
        if (identifier.isEmpty() || !Character.isLetter(identifier.charAt(0))) {
            identifier = "I_" + identifier;
        }

        String candidate = identifier;
        int suffix = 2;
        while (isTaken(candidate, taken)) {
            candidate = identifier + "_" + suffix++;
        }
        taken.add(candidate);
        return candidate;
    }

    private static boolean isTaken(String identifier, Set<String> taken) {
        for (String takenIdentifier : taken) {
            if (takenIdentifier.equalsIgnoreCase(identifier)) return true;
        }
        return false;
    }
}
