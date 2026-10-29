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

package com.dbn.assistant.mcp;

import com.dbn.common.resource.ResourceLimitPolicy;
import com.dbn.common.resource.ResourceLimits;
import lombok.experimental.UtilityClass;

@UtilityClass
public class AssistantMcpResourceLimits implements ResourceLimitPolicy {
    public static final int MAX_TOOL_COUNT = 256;
    public static final int MAX_TOOL_NAME_LENGTH = 128;
    public static final int MAX_TOOL_DESCRIPTION_LENGTH = 1_024;
    public static final int MAX_TOOL_INSTRUCTION_LENGTH = 4_096;
    public static final int MAX_TOTAL_TOOL_METADATA_LENGTH = 256_000;

    private static final ResourceLimits LIMITS = ResourceLimits.builder()
            .add("mcp-tool-count", MAX_TOOL_COUNT)
            .add("mcp-tool-name-characters", MAX_TOOL_NAME_LENGTH)
            .add("mcp-tool-description-characters", MAX_TOOL_DESCRIPTION_LENGTH)
            .add("mcp-tool-instruction-characters", MAX_TOOL_INSTRUCTION_LENGTH)
            .add("mcp-total-tool-metadata-characters", MAX_TOTAL_TOOL_METADATA_LENGTH)
            .build();

    public static boolean isToolCountAllowed(long count) {
        return LIMITS.allows("mcp-tool-count", count);
    }

    public static boolean isToolNameLengthAllowed(long length) {
        return LIMITS.allows("mcp-tool-name-characters", length);
    }

    public static boolean isToolDescriptionLengthAllowed(long length) {
        return LIMITS.allows("mcp-tool-description-characters", length);
    }

    public static boolean isToolInstructionLengthAllowed(long length) {
        return LIMITS.allows("mcp-tool-instruction-characters", length);
    }

    public static boolean isTotalToolMetadataLengthAllowed(long currentLength, long additionalLength) {
        return LIMITS.get("mcp-total-tool-metadata-characters").allowsAddition(currentLength, additionalLength);
    }
}
