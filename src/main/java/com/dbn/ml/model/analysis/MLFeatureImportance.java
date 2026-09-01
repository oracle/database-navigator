/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.ml.model.analysis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

/**
 * Oracle EXPLAIN explanatory value for one column, together with the profile of that column.
 * The value is produced by Oracle's internal model, independently of the trained model shown
 * in the result.
 *
 * @author ayoub allali
 */
@Getter
@AllArgsConstructor
public class MLFeatureImportance {
    private final String name;

    /** Explanatory power against the target, 0..1. Null when the column was not ranked. */
    private final @Nullable Double importance;

    private final @Nullable String typeName;
    private final @Nullable Long distinctValues;
    private final @Nullable String minValue;
    private final @Nullable String maxValue;

    /** Null for non numeric columns. */
    private final @Nullable Double mean;

    /** Null for non numeric columns. */
    private final @Nullable Double stdDev;
}
