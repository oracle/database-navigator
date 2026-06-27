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
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

@UtilityClass
public class Passwords {
    public static boolean verifyPassword(
            char[] password,
            int minLength,
            int maxLength,
            boolean requireLetter,
            boolean requireDigit,
            boolean requireSymbol) {
        if (!isLengthBetween(password, minLength, maxLength)) return false;
        if (requireLetter && !containsLetter(password)) return false;
        if (requireDigit && !containsDigit(password)) return false;
        if (requireSymbol && !containsSymbol(password)) return false;
        return true;
    }

    public static void clearPassword(@Nullable char[] password) {
        if (password == null) return;
        Arrays.fill(password, '\0');
    }


    public static boolean isLengthBetween(char[] password, int minLength, int maxLength) {
        if (password == null) return false;

        return password.length >= minLength && password.length <= maxLength;
    }

    public static boolean containsLetter(char[] password) {
        if (password == null) return false;

        for (char c : password) {
            if (Character.isLetter(c)) return true;
        }
        return false;
    }

    public static boolean containsDigit(char[] password) {
        if (password == null) return false;

        for (char c : password) {
            if (Character.isDigit(c)) return true;
        }
        return false;
    }

    public static boolean containsSymbol(char[] password) {
        if (password == null) return false;

        for (char c : password) {
            if (!Character.isLetterOrDigit(c)) return true;
        }
        return false;
    }
}
