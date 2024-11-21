/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.common.tracing;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

public final class ProcessingSummary {
    private final String identifier;
    private final AtomicLong invocations = new AtomicLong(0);
    private final AtomicLong failures = new AtomicLong(0);
    private final AtomicLong processingTime = new AtomicLong(0);
    private final Set<String> failureMessages = new HashSet<>();

    public ProcessingSummary(String identifier) {
        this.identifier = identifier;
    }

    public void success(long executionTime) {
        invocations.incrementAndGet();
        processingTime.addAndGet(executionTime);
    }

    public void failure(long executionTime, String message) {
        failures.incrementAndGet();
        invocations.incrementAndGet();
        processingTime.addAndGet(executionTime);
        failureMessages.add(message);
    }

    public String identifier() {
        return identifier;
    }

    public long invocations() {
        return invocations.get();
    }

    public long getFailures() {
        return failures.get();
    }

    public long totalProcessingTime() {
        return processingTime.get();
    }

    public long averageProcessingTime() {
        long invocations = invocations();
        if (invocations == 0) {
            return 0;
        }
        long totalProcessingTime = totalProcessingTime();
        return totalProcessingTime / invocations;
    }
}
