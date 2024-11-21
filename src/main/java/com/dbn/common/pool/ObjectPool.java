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

import java.util.concurrent.TimeUnit;

public interface ObjectPool<T, E extends Throwable> {
    /**
     * Acquire an object from the pool
     *
     * @param timeout time to give up
     * @param timeUnit the unit of time to give op
     * @return an object from the pool
     * @throws E when the pool reached limits or failed to initialize the object
     */
    T acquire(long timeout, TimeUnit timeUnit) throws E;

    /**
     * Release the object back to the pool
     * The object will be made available for the next {@link #acquire(long, TimeUnit)} operation
     *
     * @param object the object to be released
     * @return the released object
     */
    T release(T object);

    /**
     * Drop an object from the pool
     *
     * @param object the object to be removed
     * @return the removed object
     */
    T drop(T object);

    int size();

    int maxSize();

    int peakSize();

    default boolean isEmpty() {
        return size() == 0;
    }
}
