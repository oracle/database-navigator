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
import lombok.extern.slf4j.Slf4j;

import java.util.Base64.Decoder;
import java.util.Base64.Encoder;

/**
 * Base64 string utilities
 */
@Slf4j
@UtilityClass
public final class Base64 {

    public static char[] encode(char[] chars) {
        return Chars.fromString(encode(Chars.toString(chars)));
    }

    public static char[] decode(char[] chars) {
        return Chars.fromString(decode(Chars.toString(chars)));
    }

    public static String encode(String string) {
        if (Strings.isEmpty(string)) return "";
        try {
            Encoder encoder = java.util.Base64.getEncoder();
            byte[] bytes = string.getBytes();
            byte[] encoded = encoder.encode(bytes);
            return new String(encoded);
        } catch (Exception e) {
            log.warn("Failed to encode string. Returning original", e);
        }
        return string;
    }

    public static String decode(String string) {
        if (Strings.isEmpty(string)) return "";

        try {
            Decoder decoder = java.util.Base64.getDecoder();
            byte[] bytes = string.getBytes();
            byte[] decoded = decoder.decode(bytes);
            return new String(decoded);
        } catch (Exception e) {
            log.warn("Failed to decode string. Returning original", e);
        }

        return string;
    }
}
