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

package com.dbn.database.interfaces.queue;

import com.dbn.common.count.Counter;
import com.dbn.common.count.CounterType;
import com.dbn.common.count.Counters;

public class InterfaceThreadMonitor {
    private static final Counters progressThreads = new Counters();
    private static final Counters backgroundThreads = new Counters();

    public static int getRunningThreadCount(boolean progress) {
        return getCounter(CounterType.RUNNING, progress).get();
    }

    public static Counter getCounter(CounterType counterType, boolean progress) {
        return progress ?
                progressThreads.get(counterType) :
                backgroundThreads.get(counterType);
    }

    public static void start(boolean progress) {
        int count = getCounter(CounterType.RUNNING, progress).increment();
        getCounter(CounterType.PEAK, progress).max(count);
    }

    public static void finish(boolean progress) {
        getCounter(CounterType.RUNNING, progress).decrement();
        getCounter(CounterType.FINISHED, progress).increment();
    }
}
