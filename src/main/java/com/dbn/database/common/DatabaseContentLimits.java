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

package com.dbn.database.common;

import lombok.experimental.UtilityClass;

import java.sql.SQLException;

@UtilityClass
public class DatabaseContentLimits {
    public static final int MAX_SOURCE_LINE_COUNT = 1_000_000;
    public static final int MAX_SOURCE_TEXT_LENGTH = 64 * 1024 * 1024;
    public static final int MAX_JAVA_BINARY_LENGTH = 64 * 1024 * 1024;

    public static void checkSourceLineCount(long lineCount) throws SQLException {
        checkCount(lineCount, MAX_SOURCE_LINE_COUNT, "Database source code line count");
    }

    public static void checkSourceTextLength(long length) throws SQLException {
        checkLength(length, MAX_SOURCE_TEXT_LENGTH, "Database source code");
    }

    public static void checkJavaBinaryLength(long length) throws SQLException {
        checkLength(length, MAX_JAVA_BINARY_LENGTH, "Database Java binary");
    }

    public static void checkLength(long length, long maxLength, String contentName) throws SQLException {
        if (length <= maxLength) return;
        throw new SQLException(contentName + " exceeds the maximum supported size of " + formatSize(maxLength));
    }

    public static void checkCount(long count, long maxCount, String contentName) throws SQLException {
        if (count <= maxCount) return;
        throw new SQLException(contentName + " exceeds the maximum supported count of " + maxCount);
    }

    private static String formatSize(long size) {
        return (size / 1024 / 1024) + " MB";
    }
}
