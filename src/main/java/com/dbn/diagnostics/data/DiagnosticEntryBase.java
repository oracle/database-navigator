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

package com.dbn.diagnostics.data;

import lombok.Getter;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

@Getter
public class DiagnosticEntryBase<T> implements DiagnosticEntry<T> {
    private final T identifier;
    private final String qualifier = DEFAULT_QUALIFIER;
    private final AtomicLong invocations = new AtomicLong();
    private final AtomicLong failures = new AtomicLong();
    private final AtomicLong timeouts = new AtomicLong();

    private final AtomicLong total = new AtomicLong();
    private final AtomicLong best = new AtomicLong();
    private final AtomicLong worst = new AtomicLong();

    public DiagnosticEntryBase(T identifier) {
        this.identifier = identifier;
    }

    @Override
    public boolean isComposite() {
        return false;
    }

    @Override
    public DiagnosticEntry<T> getDetail(String qualifier) {
        if (!Objects.equals(this.qualifier, qualifier))
            throw new IllegalArgumentException("Only supported for composites");

        return this;
    }

    @Override
    public long getInvocations() {
        return invocations.get();
    }

    @Override
    public long getFailures() {
        return failures.get();
    }

    @Override
    public long getTimeouts() {
        return timeouts.get();
    }

    @Override
    public long getTotal() {
        return total.get();
    }

    @Override
    public long getAverage() {
        long totalTime = getTotal();
        long invocations = getInvocations();
        return invocations == 0 ? 0 : totalTime / invocations;
    }

    @Override
    public long getBest() {
        return best.get();
    }

    @Override
    public long getWorst() {
        return worst.get();
    }

    @Override
    public void log(boolean failure, boolean timeout, long value) {
        invocations.incrementAndGet();
        if (failure) {
            failures.incrementAndGet();
        }
        if (timeout) {
            timeouts.incrementAndGet();
        }

        if (best.get() == 0 || best.get() > value) {
            best.set(value);
        }
        if (worst.get() == 0 || worst.get() < value) {
            worst.set(value);
        }

        total.addAndGet(value);
    }


}
