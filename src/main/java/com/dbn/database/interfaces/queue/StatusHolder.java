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

import java.util.Stack;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class StatusHolder<T extends InterfaceTaskStatus> {
    private final Stack<T> history = new Stack<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private T status;

    public StatusHolder(T initial) {
        this.status = initial;
    }

    public Stack<T> getHistory() {
        return history;
    }

    boolean change(T status) {
        Lock writeLock = lock.writeLock();
        try {
            writeLock.lock();

            if (!status.isAfter(this.status)) {
                // violation
                return false;
            }

            this.history.push(this.status);
            this.status = status;
            return true;
        } finally {
            writeLock.unlock();
        }
    }

    public T get() {
        Lock readLock = lock.readLock();
        try {
            readLock.lock();
            return status;
        } finally {
            readLock.unlock();
        }
    }

    public boolean is(T status) {
        return get() == status;
    }

    public boolean isBefore(T status) {
        return this.status.compareTo(status) < 0;
    }

    @Override
    public String toString() {
        return status.toString();
    }
}
