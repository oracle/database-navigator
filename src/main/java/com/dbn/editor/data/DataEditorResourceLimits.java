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

package com.dbn.editor.data;

import com.dbn.common.resource.ResourceLimits;
import lombok.experimental.UtilityClass;

/**
 * Resource bounds for data editor fetching and value lists.
 */
@UtilityClass
public class DataEditorResourceLimits {
    public static final int MIN_DATA_EDITOR_FETCH_BLOCK_SIZE = 1;
    public static final int MAX_DATA_EDITOR_FETCH_BLOCK_SIZE = 10000;
    public static final int DEFAULT_DATA_EDITOR_FETCH_BLOCK_SIZE = 100;

    public static final int MIN_VALUE_LIST_THRESHOLD = 0;
    public static final int MAX_VALUE_LIST_THRESHOLD = 10000;
    public static final int DEFAULT_VALUE_LIST_THRESHOLD = 1000;

    public static int normalizeDataEditorFetchBlockSize(long value) {
        return (int) ResourceLimits.clampToRange(value,
                MIN_DATA_EDITOR_FETCH_BLOCK_SIZE, MAX_DATA_EDITOR_FETCH_BLOCK_SIZE);
    }

    public static int normalizeDataEditorReloadRowCount(long requestedRows, int loadedRows) {
        // Reloads may restore rows already loaded through explicit user fetches.
        return Math.max(normalizeDataEditorFetchBlockSize(requestedRows), loadedRows);
    }

    public static int normalizeValueListThreshold(long value) {
        return (int) ResourceLimits.clampToRange(value,
                MIN_VALUE_LIST_THRESHOLD, MAX_VALUE_LIST_THRESHOLD);
    }

    public static int getDistinctValuesFetchLimit(int valuesThreshold) {
        // One extra value detects whether the list exceeds the configured threshold.
        return normalizeValueListThreshold(valuesThreshold) + 1;
    }
}
