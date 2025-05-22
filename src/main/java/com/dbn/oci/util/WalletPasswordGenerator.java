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

package com.dbn.oci.util;

import lombok.experimental.UtilityClass;

import java.util.Random;

@UtilityClass
public class WalletPasswordGenerator {

    private static final String LETTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String NUMBERS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{}|;:,.<>?/";
    private static final String ALL_CHARACTERS = LETTERS + NUMBERS + SYMBOLS;

    public static String generateRandomPassword() {
        // Define character pools

        Random random = new Random();
        StringBuilder password = new StringBuilder();

        // Add at least one letter
        password.append(randomChar(LETTERS, random));

        // Add at least one number
        password.append(randomChar(NUMBERS, random));

        // Add at least one special character
        password.append(randomChar(SYMBOLS, random));

        // Combine all character pools

        // Fill the remaining characters up to 8
        while (password.length() < 8) {
            password.append(randomChar(ALL_CHARACTERS, random));
        }

        // Shuffle the password
        char[] passwordArray = password.toString().toCharArray();
        for (int i = passwordArray.length - 1; i > 0; i--) {
            int index = random.nextInt(i + 1);
            char temp = passwordArray[i];
            passwordArray[i] = passwordArray[index];
            passwordArray[index] = temp;
        }

        // Return the shuffled password as a string
        return new String(passwordArray);
    }

    private static char randomChar(String letters, Random random) {
        return letters.charAt(random.nextInt(letters.length()));
    }
}
