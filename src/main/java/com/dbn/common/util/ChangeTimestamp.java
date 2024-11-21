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

package com.dbn.common.util;

import org.jetbrains.annotations.NotNull;

import java.sql.Timestamp;

public class ChangeTimestamp {
    private final Timestamp value;
    private final long captureTime;

    public ChangeTimestamp() {
        this.captureTime = System.currentTimeMillis();
        this.value = new Timestamp(captureTime);
    }
    public ChangeTimestamp(@NotNull Timestamp value) {
        this.value = value;
        this.captureTime = System.currentTimeMillis();
    }

    public static ChangeTimestamp of(@NotNull Timestamp value) {
        return new ChangeTimestamp(value);
    }

    public static ChangeTimestamp now() {
        return new ChangeTimestamp(new Timestamp(System.currentTimeMillis()));
    }

    @NotNull
    public Timestamp value() {
        return value;
    }

    public boolean isDirty() {
        return TimeUtil.isOlderThan(captureTime, 30 * TimeUtil.Millis.ONE_SECOND);
    }

    public boolean isOlderThan(ChangeTimestamp changeTimestampCheck) {
        return value.before(changeTimestampCheck.value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
