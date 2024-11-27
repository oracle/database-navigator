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

package com.dbn.database;

import lombok.Getter;

import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import static com.dbn.common.util.TimeUtil.isOlderThan;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

public class DatabaseActivityTrace {
    private boolean supported = true;
    private int failedAttempts;
    private long lastAttempt;

    @Getter
    private SQLException exception;

    public void init() {
        lastAttempt = System.currentTimeMillis();
        if (retryIntervalLapsed()) reset();
    }

    public void fail(SQLException exception, boolean unsupported) {
        this.exception = exception;
        this.failedAttempts++;
        if (unsupported) this.supported = false;
    }

    public void release() {
        if (exception == null) reset();
    }

    public boolean canExecute() {
        // do not allow more than three attempts per retry-interval
        return failedAttempts < 3 || retryIntervalLapsed();
    }

    private void reset() {
        supported = true;
        exception = null;
        failedAttempts = 0;
    }

    private boolean retryIntervalLapsed() {
        // increase retry-interval for activities provisionally marked as unsupported
        // (e.g. missing grants to given system views translating as syntax error)
        TimeUnit delayUnit = supported ? SECONDS : MINUTES;
        return isOlderThan(lastAttempt, 5, delayUnit);
    }

}
