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

package com.dbn.connection;

import com.dbn.common.resource.ResourceLimits;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Resource ceilings and admission checks for connection configuration.
 */
@UtilityClass
public class ConnectionResourceLimits {
    public static final int MAX_CONNECTION_COUNT = 256;
    public static final long MAX_CLIPBOARD_XML_SIZE_BYTES = 10L * 1024 * 1024;

    private static final @NonNls String CLIPBOARD_XML_CHARACTERS = "clipboard XML characters";
    private static final @NonNls String CLIPBOARD_XML_BYTES = "clipboard XML bytes";
    private static final @NonNls String CONNECTION_COUNT = "connection count";

    private static final ResourceLimits LIMITS = ResourceLimits.builder()
            .add(CLIPBOARD_XML_CHARACTERS, MAX_CLIPBOARD_XML_SIZE_BYTES)
            .add(CLIPBOARD_XML_BYTES, MAX_CLIPBOARD_XML_SIZE_BYTES)
            .add(CONNECTION_COUNT, MAX_CONNECTION_COUNT)
            .build();

    public static boolean isClipboardImportAllowed(@NotNull String content) {
        return LIMITS.allows(CLIPBOARD_XML_CHARACTERS, content.length()) &&
                LIMITS.allows(CLIPBOARD_XML_BYTES, ResourceLimits.getUtf8ByteCount(content, MAX_CLIPBOARD_XML_SIZE_BYTES));
    }

    public static boolean isConnectionImportAllowed(long currentCount, long importedCount) {
        return LIMITS.get(CONNECTION_COUNT).allowsAddition(currentCount, importedCount);
    }

}
