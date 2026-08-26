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

package com.dbn.assistant.tool.execution;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

import static com.dbn.common.util.Commons.nvl;

@UtilityClass
public class AssistantToolRequestLimits {
    public static final int MAX_TOOL_REQUEST_ARGUMENT_LENGTH = 64 * 1024;
    public static final int MAX_TOOL_REQUEST_PREVIEW_LENGTH = 64 * 1024;
    public static final int MAX_TOOL_REQUEST_ARGUMENT_COUNT = 16;
    public static final int MAX_FUZZY_ARGUMENT_NAME_LENGTH = 256;
    public static final int MAX_FUZZY_ARGUMENT_TEXT_LENGTH = 512;
    public static final int MAX_SIMPLIFIED_ARGUMENT_NAME_LENGTH = 128;

    private static final String TRUNCATED_SUFFIX = "\n[Tool request arguments truncated: original length %d characters]";

    public static boolean isOversized(String content) {
        return nvl(content, "").length() > MAX_TOOL_REQUEST_ARGUMENT_LENGTH;
    }

    public static boolean isPreviewOversized(String content) {
        return nvl(content, "").length() > MAX_TOOL_REQUEST_PREVIEW_LENGTH;
    }

    public static String createPreview(String content, int originalLength) {
        String value = nvl(content, "");
        String suffix = String.format(TRUNCATED_SUFFIX, originalLength);
        int length = Math.max(0, MAX_TOOL_REQUEST_PREVIEW_LENGTH - suffix.length());
        return value.substring(0, Math.min(value.length(), length)) + suffix;
    }

    @NonNls
    public static String getOversizedRequestMessage(int originalLength) {
        return "Tool request arguments exceeded the maximum allowed size of " +
                MAX_TOOL_REQUEST_ARGUMENT_LENGTH + " characters (received " + originalLength + "). " +
                "Retry with a narrower request, more specific filters, or smaller argument values.";
    }
}
