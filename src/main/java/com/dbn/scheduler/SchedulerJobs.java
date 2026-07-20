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

package com.dbn.scheduler;

import com.dbn.common.util.UUIDs;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

@UtilityClass
public class SchedulerJobs {
    private static final int MAX_JOB_NAME_LENGTH = 30;
    private static final int MAX_PREFIX_LENGTH = 12;

    @NotNull
    public String newJobName(@NotNull String prefix) {
        String normalizedPrefix = prefix
                .replaceAll("[^A-Za-z0-9_]", "_")
                .toUpperCase(Locale.ROOT);
        normalizedPrefix = normalizedPrefix.substring(0, Math.min(normalizedPrefix.length(), MAX_PREFIX_LENGTH));

        String base = "DBN_" + normalizedPrefix + "_";
        int identifierLength = MAX_JOB_NAME_LENGTH - base.length();
        return base + UUIDs.compact().substring(0, identifierLength);
    }
}
