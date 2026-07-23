/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 */

package com.dbn.common.util;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/** Captures the start, completion, and elapsed time of an operation. */
@Getter
public class ExecutionTiming {
    private long startTime;
    private long endTime;

    public void start() {
        startTime = System.currentTimeMillis();
        endTime = 0;
    }

    public void finish() {
        endTime = System.currentTimeMillis();
    }

    @NotNull
    public Duration getDuration() {
        if (startTime == 0) return Duration.ZERO;
        long finishTime = endTime > 0 ? endTime : System.currentTimeMillis();
        return Duration.ofMillis(Math.max(0, finishTime - startTime));
    }
}
