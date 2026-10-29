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

package com.dbn.data.value;

import com.dbn.common.resource.ResourceLimitPolicy;
import com.dbn.common.resource.ResourceLimits;
import lombok.experimental.UtilityClass;

@UtilityClass
public class LargeValueResourceLimits implements ResourceLimitPolicy {
    public static final int MAX_READ_SIZE = 1024 * 1024;

    private static final ResourceLimits LIMITS = ResourceLimits.builder()
            .add("large-value-read-units", MAX_READ_SIZE)
            .build();

    public static int normalizeReadSize(int requestedSize) {
        return requestedSize <= 0 ? MAX_READ_SIZE : (int) LIMITS.get("large-value-read-units").clamp(requestedSize);
    }

    public static boolean isReadSizeAllowed(long size) {
        return LIMITS.allows("large-value-read-units", size);
    }
}
