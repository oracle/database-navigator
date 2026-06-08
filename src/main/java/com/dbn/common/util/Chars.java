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

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Basic char array utilities
 */
@UtilityClass
public class Chars {
    public static final char[] EMPTY_ARRAY = new char[0];

    public static boolean isEmpty(char[] chars) {
        return chars == null || chars.length == 0;
    }

    public static boolean isNotEmpty(char[] chars) {
        return !isEmpty(chars);
    }

    public static String toString(char[] chars) {
        return chars == null ? null : new String(chars);
    }

    public static String toStringAcceptEmpty(char[] chars) {
        return chars == null ? new String(EMPTY_ARRAY) : new String(chars);
    }

    public static char[] fromString(String string) {
        return string == null ? null : string.toCharArray();
    }

    public static byte[] toBytes(char[] chars) {
        if (chars == null) return null;

        ByteBuffer buffer = UTF_8.encode(CharBuffer.wrap(chars));
        try {
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            return bytes;
        } finally {
            clear(buffer);
        }
    }

    public static char[] fromBytes(byte[] bytes) {
        if (bytes == null) return null;

        CharBuffer buffer = UTF_8.decode(ByteBuffer.wrap(bytes));
        try {
            char[] chars = new char[buffer.remaining()];
            buffer.get(chars);
            return chars;
        } finally {
            clear(buffer);
        }
    }

    public static void clear(char[] chars) {
        if (chars != null) {
            Arrays.fill(chars, ' ');
        }
    }

    public static void clear(byte[] bytes) {
        if (bytes != null) {
            Arrays.fill(bytes, (byte) 0);
        }
    }

    private static void clear(ByteBuffer buffer) {
        if (buffer != null && buffer.hasArray()) {
            clear(buffer.array());
        }
    }

    private static void clear(CharBuffer buffer) {
        if (buffer != null && buffer.hasArray()) {
            clear(buffer.array());
        }
    }
}
