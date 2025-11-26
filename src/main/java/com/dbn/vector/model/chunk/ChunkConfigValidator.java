/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.vector.model.chunk;

import lombok.experimental.UtilityClass;

@UtilityClass
public class ChunkConfigValidator {
    /**
     * Validates the maxSize according to Oracle VECTOR_CHUNKS rules.
     */
    public static String validateMaxSize(String chunkBy, int maxSize) {
        if (chunkBy == null) return null;
        int minAllowed;
        int maxAllowed;

        switch (chunkBy) {
            case "CHARACTERS":
            case "CHARS":
                minAllowed = 50;
                maxAllowed = 4000;
                break;
            case "WORDS":
                minAllowed = 10;
                maxAllowed = 1000;
                break;
            case "VOCABULARY":
                minAllowed = 10;
                maxAllowed = 1000;
                break;
            default: return null;
        }

        if (maxSize < minAllowed || maxSize > maxAllowed) {
            return String.format(
                    "Invalid max size %d for chunk by %s. Expected a value between %d and %d",
                    maxSize, chunkBy, minAllowed, maxAllowed
            );
        }
        return null;
    }

    /**
     * Validates the overlap according to Oracle VECTOR_CHUNKS rules:
     * - 0 is allowed
     * - otherwise must be between 5% and 20% of maxSize
     */
    public static String validateOverlap(int maxSize, int overlap) {
        // 0 is always valid
        if (overlap == 0) return null;

        double minOverlap = maxSize * 0.05;
        double maxOverlap = maxSize * 0.20;

        if (overlap < minOverlap || overlap > maxOverlap) {
            return String.format(
                    "Invalid overlap %d for max size %d. Expected a value between 5%% (%.1f) and 20%% (%.1f) of max size.",
                    overlap, maxSize, minOverlap, maxOverlap);
        }
        return null;
    }
}
