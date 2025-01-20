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

package com.dbn.common.pool;

import com.dbn.common.count.Counter;
import com.dbn.common.count.CounterType;
import com.dbn.common.count.Counters;
import org.jetbrains.annotations.NonNls;

public class ObjectPoolCounters extends Counters {
    public Counter peak() {
        return get(CounterType.PEAK);
    }

    public Counter waiting() {
        return get(CounterType.WAITING);
    }

    public Counter reserved() {
        return get(CounterType.RESERVED);
    }

    public Counter rejected() {
        return get(CounterType.REJECTED);
    }

    public Counter creating() {
        return get(CounterType.CREATING);
    }

    @NonNls
    @Override
    public String toString() {
        return
            "peak=" + peak().get() + " " +
            "waiting=" + waiting().get() + " " +
            "reserved=" + reserved().get() + " " +
            "rejected=" + rejected().get() + " " +
            "creating=" + creating().get()
                ;
    }
}
