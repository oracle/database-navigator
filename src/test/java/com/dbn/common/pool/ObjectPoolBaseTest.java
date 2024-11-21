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

import com.dbn.common.dispose.StatefulDisposableBase;
import org.junit.Assert;
import org.junit.Test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

public class ObjectPoolBaseTest {
    private static final AtomicInteger counter = new AtomicInteger();
    public static final int POOL_SIZE = 10;
    private static final Random random = new Random();
    private final AtomicInteger acquireCounter = new AtomicInteger();
    private final AtomicInteger releaseCounter = new AtomicInteger();
    private final AtomicInteger rejectCounter = new AtomicInteger();
    private final AtomicInteger dropCounter = new AtomicInteger();

    private final ObjectPool<TestObject, Exception> objectPool = new ObjectPoolBase<>(null) {
        @Override
        protected TestObject create() {
            TestObject object = new TestObject(counter.incrementAndGet());
            System.out.println("Created " + object);
            return object;
        }

        @Override
        protected boolean check(TestObject object) {
            return System.currentTimeMillis() - object.timestamp < 2000;
        }

        @Override
        public int maxSize() {
            return POOL_SIZE;
        }

        @Override
        protected TestObject whenErrored(Throwable e) {
            throw new RuntimeException(e);
        }

        @Override
        protected TestObject whenNull() {
            System.out.println("Rejected");
            rejectCounter.incrementAndGet();
            return null;
        }

        @Override
        protected TestObject whenAcquired(TestObject object) {
            System.out.println("Acquired " + object);
            acquireCounter.incrementAndGet();
            return object;
        }

        @Override
        protected TestObject whenDropped(TestObject object) {
            System.out.println("Dropped " + object);
            dropCounter.incrementAndGet();
            return object;
        }

        @Override
        protected TestObject whenReleased(TestObject object) {
            System.out.println("Released " + object);
            releaseCounter.incrementAndGet();
            return object;
        }
    };

    @Test
    public void exhaustPool() throws Exception{
        Thread invoker = Thread.currentThread();
        ExecutorService executorService = Executors.newCachedThreadPool();
        int iterations = 100;
        AtomicInteger counter = new AtomicInteger();

        for (int i=0; i<iterations; i++) {
            int delay = random.nextInt(100);
            Thread.sleep(delay);

            executorService.submit(() -> {
                TestObject object = null;
                try {
                    int timeout = random.nextInt(5);
                    object = objectPool.acquire(timeout, TimeUnit.SECONDS);

                    int process = random.nextInt(2000);
                    Thread.sleep(process);
                    if (counter.incrementAndGet() >= iterations) {
                        LockSupport.unpark(invoker);
                        System.out.println("UNPARKED");
                    }

                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    if (object != null) {
                        objectPool.release(object);
                    }
                }
            });
        }

        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(120));

        Assert.assertEquals(acquireCounter.get(), releaseCounter.get() + dropCounter.get());
        Assert.assertEquals(acquireCounter.get() + rejectCounter.get(), iterations);
    }


    private static class TestObject extends StatefulDisposableBase {
        private final long timestamp = System.currentTimeMillis();
        private final int index;
        private boolean disposed;

        private TestObject(int index) {
            this.index = index;
        }

        @Override
        public String toString() {
            return "Object " + index;
        }

        @Override
        public void disposeInner() {

        }
    }
}