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

package com.dbn.common.constant;

import com.dbn.common.resource.ResourceLimitPolicy;
import com.dbn.common.resource.ResourceLimits;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * Bounds for IDs retained by each pseudo-constant type.
 */
@UtilityClass
public class PseudoConstantResourceLimits implements ResourceLimitPolicy {
    public static final int MAX_ID_LENGTH = 1024;
    public static final int MAX_REGISTERED_ID_COUNT = 65536;

    private static final @NonNls String ID_LENGTH = "pseudo-constant ID length";
    private static final @NonNls String REGISTERED_ID_COUNT = "registered pseudo-constant ID count";

    private static final ResourceLimits LIMITS = ResourceLimits.builder()
            .add(ID_LENGTH, MAX_ID_LENGTH)
            .add(REGISTERED_ID_COUNT, MAX_REGISTERED_ID_COUNT)
            .build();

    public static void verifyPseudoConstantId(@NotNull String id) {
        LIMITS.verify(ID_LENGTH, id.length());
    }

    public static void verifyPseudoConstantRegistration(long currentCount) {
        LIMITS.get(REGISTERED_ID_COUNT).verifyAddition(currentCount, 1);
    }
}
