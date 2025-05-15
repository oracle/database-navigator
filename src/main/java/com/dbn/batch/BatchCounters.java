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

package com.dbn.batch;

import com.dbn.common.count.Counter;
import com.dbn.common.count.Counters;
import org.jetbrains.annotations.NonNls;

import static com.dbn.common.count.CounterType.FAILURE;
import static com.dbn.common.count.CounterType.QUEUED;
import static com.dbn.common.count.CounterType.SUCCESS;

public class BatchCounters extends Counters {
    public int queuedItems() {
        return queued().get();
    }

    public int successItems() {
        return success().get();
    }

    public int failedItems() {
        return failure().get();
    }

    public int processedItems() {
        return success().get() + failure().get();
    }

    public Counter queued() {
        return get(QUEUED);
    }

    public Counter success() {
        return get(SUCCESS);
    }

    public Counter failure() {
        return get(FAILURE);
    }

    @NonNls
    @Override
    public String toString() {
        return
            "success=" + success().get() + " " +
            "failure=" + failure().get();
    }
}
