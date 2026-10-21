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

package com.dbn.execution;

import com.dbn.common.resource.ResourceLimits;
import lombok.experimental.UtilityClass;

/**
 * Resource bounds for execution result fetching.
 */
@UtilityClass
public class ExecutionResourceLimits {
    public static final int MIN_RESULT_SET_FETCH_BLOCK_SIZE = 1;
    public static final int MAX_RESULT_SET_FETCH_BLOCK_SIZE = 10000;
    public static final int DEFAULT_RESULT_SET_FETCH_BLOCK_SIZE = 100;

    public static int normalizeResultSetFetchBlockSize(long value) {
        return (int) ResourceLimits.clampToRange(value,
                MIN_RESULT_SET_FETCH_BLOCK_SIZE, MAX_RESULT_SET_FETCH_BLOCK_SIZE);
    }
}
