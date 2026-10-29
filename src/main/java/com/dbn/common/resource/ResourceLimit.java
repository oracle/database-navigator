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

package com.dbn.common.resource;

public final class ResourceLimit {
    private final String name;
    private final long maximum;

    ResourceLimit(String name, long maximum) {
        this.name = name;
        this.maximum = maximum;
    }

    public String getName() {
        return name;
    }

    public long getMaximum() {
        return maximum;
    }

    public boolean allows(long value) {
        return value >= 0 && value <= maximum;
    }

    public long verify(long value) {
        if (!allows(value)) {
            throw new IllegalArgumentException(name + " exceeds its maximum of " + maximum + " (received " + value + ")");
        }
        return value;
    }

    public long clamp(long value) {
        return Math.max(0, Math.min(value, maximum));
    }

    public long remaining(long consumed) {
        verify(consumed);
        return maximum - consumed;
    }

    public boolean allowsAddition(long current, long additional) {
        return current >= 0 && additional >= 0 && current <= maximum && additional <= maximum - current;
    }

    public long verifyAddition(long current, long additional) {
        if (!allowsAddition(current, additional)) {
            throw new IllegalArgumentException(name + " exceeds its maximum of " + maximum);
        }
        return current + additional;
    }

    public boolean allowsProduct(long left, long right) {
        return left >= 0 && right >= 0 && (left == 0 || right <= maximum / left);
    }

    public long verifyProduct(long left, long right) {
        if (!allowsProduct(left, right)) {
            throw new IllegalArgumentException(name + " exceeds its maximum of " + maximum);
        }
        return left * right;
    }
}
