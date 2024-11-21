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

import lombok.experimental.UtilityClass;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


@UtilityClass
public class Characters {
    private static final Map<Character, Character> UPPER_CASE_CHARS = new ConcurrentHashMap<>();
    private static final Map<Character, Character> LOWER_CASE_CHARS = new ConcurrentHashMap<>();

    public static char toUpperCase(char chr){
        return UPPER_CASE_CHARS.computeIfAbsent(chr, c -> Strings.toUpperCase(String.valueOf(c)).charAt(0));
    }

    public static char toLowerCase(char chr){
        return LOWER_CASE_CHARS.computeIfAbsent(chr, c -> Strings.toLowerCase(String.valueOf(c)).charAt(0));
    }

    public static boolean equalIgnoreCase(char char1, char char2) {
        return Objects.equals(toUpperCase(char1), toUpperCase(char2));
    }

}
