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

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Getter
public class WordTokenizer {
    private static final Pattern PATTERN2 = Pattern.compile("\\B");
    private static final Pattern PATTERN1 = Pattern.compile("\\b");
    private final List<String> tokens = new ArrayList<>();

    public WordTokenizer(String string) {
        //String[] allTokens = string.split("\\b\\s+|\\s+\\b|\\b");
        String[] tokens1 = PATTERN1.split(string);
        for (String token1 : tokens1) {
            token1 = token1.trim();
            if (token1.isEmpty()) continue;

            if (isSplittableToken(token1)) {
                String[] tokens2 = PATTERN2.split(token1);
                for (String token2 : tokens2) {
                    token2 = token2.trim();
                    if (token2.isEmpty()) continue;

                    tokens.add(token2);
                }
            } else {
                tokens.add(token1);
            }
        }

    }
    
    private static boolean isSplittableToken(String token) {
        if (token.length() > 1) {
            char chr = token.charAt(0);
            return !Character.isLetter(chr) && !Character.isDigit(chr) && chr != '_';            
        }
        return false;
    }
}
