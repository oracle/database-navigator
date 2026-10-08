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

import com.intellij.openapi.progress.ProcessCanceledException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ObjectCacheBaseTest {
    private final List<TestCache> caches = new ArrayList<>();
    private final Map<FutureTask<?>, Thread> workers = new IdentityHashMap<>();

    @After
    public void dispose() throws InterruptedException {
        for (Thread worker : workers.values()) worker.interrupt();
        try {
            for (Thread worker : workers.values()) {
                worker.join(TimeUnit.SECONDS.toMillis(5));
                Assert.assertFalse("Cache worker did not stop", worker.isAlive());
            }
        } finally {
            for (TestCache cache : caches) cache.dispose();
        }
    }

    @Test
    public void reusesCachedObjectsAndDiscardsThemOnce() throws Exception {
        TestCache cache = cache(new TestCache() {
            @Override
            protected TestObject whenDiscarded(TestObject object) {
                super.whenDiscarded(object);
                Assert.assertNotSame(object, get(object.key));
                discard(object);
                return object;
            }
        });
        TestObject object = cache.ensure("main");
        Assert.assertSame(object, cache.ensure("main"));
        Assert.assertEquals(1, cache.created.size());

        cache.discard(object);
        Assert.assertNull(cache.get("main"));
        Assert.assertEquals(1, object.discards.get());

        TestObject replacement = cache.ensure("main");
        cache.discard(replacement);
        cache.discard(replacement);
        Assert.assertEquals(1, replacement.discards.get());
        Assert.assertEquals(0, cache.size());
    }

    @Test
    public void staleCallbacksCannotRemoveAnEqualReplacement() throws Exception {
        TestCache cache = cache(new TestCache());
        TestObject first = cache.ensure("main");
        cache.discard(first);
        TestObject second = cache.ensure("main");
        Assert.assertEquals(first, second);
        Assert.assertNotSame(first, second);

        cache.discard(first);
        cache.discard(null);

        Assert.assertSame(second, cache.get("main"));
        Assert.assertEquals(1, first.discards.get());
        Assert.assertEquals(0, second.discards.get());
    }

    @Test
    public void validationCanDiscardItsOwnEntry() throws Exception {
        TestCache cache = cache(new TestCache() {
            @Override
            protected boolean check(TestObject object) {
                if (!object.valid) discard(object);
                return super.check(object);
            }
        });
        TestObject first = cache.ensure("main");
        first.valid = false;

        TestObject second = cache.ensure("main");

        Assert.assertNotSame(first, second);
        Assert.assertSame(second, cache.get("main"));
        Assert.assertEquals(1, first.discards.get());
        Assert.assertEquals(1, cache.size());
    }

    @Test
    public void concurrentCreationIsSharedPerKeyAndIndependentAcrossKeys() throws Exception {
        CountDownLatch creating = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        TestCache cache = cache(new TestCache() {
            @Override
            protected TestObject create(String key) throws Exception {
                if (key.equals("main")) {
                    creating.countDown();
                    await(proceed);
                }
                return super.create(key);
            }
        });
        FutureTask<TestObject> first = start(() -> cache.ensure("main"));
        await(creating);
        FutureTask<TestObject> second = start(() -> cache.ensure("main"));
        awaitWaiting(second);
        Assert.assertNull(cache.get("main"));
        Assert.assertEquals(0, cache.size());

        Assert.assertNotNull(result(start(() -> cache.ensure("debug"))));
        proceed.countDown();
        Assert.assertSame(result(first), result(second));
        Assert.assertEquals(2, cache.size());
        Assert.assertEquals(2, cache.created.size());
    }

    @Test
    public void creationNotificationsCanLookUpThePublishedObject() throws Exception {
        TestCache cache = cache(new TestCache() {
            @Override
            protected TestObject whenCreated(TestObject object) {
                Assert.assertSame(object, get(object.key));
                try {
                    Assert.assertSame(object, ensure(object.key));
                } catch (Exception e) {
                    throw new AssertionError(e);
                }
                return object;
            }
        });

        Assert.assertNotNull(result(start(() -> cache.ensure("main"))));
        Assert.assertEquals(1, cache.created.size());
    }

    @Test
    public void failedCreationWakesWaitersToRetry() throws Exception {
        CountDownLatch creating = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        IllegalStateException failure = new IllegalStateException("creation failure");
        TestCache cache = cache(new TestCache() {
            private final AtomicBoolean first = new AtomicBoolean(true);

            @Override
            protected TestObject create(String key) throws Exception {
                if (first.getAndSet(false)) {
                    creating.countDown();
                    await(proceed);
                    throw failure;
                }
                return super.create(key);
            }
        });
        FutureTask<TestObject> first = start(() -> cache.ensure("main"));
        await(creating);
        FutureTask<TestObject> second = start(() -> cache.ensure("main"));
        awaitWaiting(second);
        proceed.countDown();

        Assert.assertSame(failure, Assert.assertThrows(ExecutionException.class,
                () -> result(first)).getCause());
        Assert.assertSame(result(second), cache.get("main"));
        Assert.assertEquals(1, cache.size());
    }

    @Test
    public void nullCreationFailsWithoutLeavingAPhantomEntry() throws Exception {
        TestCache cache = cache(new TestCache() {
            private boolean first = true;

            @Override
            protected TestObject create(String key) throws Exception {
                if (first) {
                    first = false;
                    return null;
                }
                return super.create(key);
            }
        });

        Assert.assertThrows(IllegalStateException.class, () -> cache.ensure("main"));
        Assert.assertEquals(0, cache.size());
        Assert.assertNotNull(cache.ensure("main"));
        Assert.assertEquals(1, cache.size());
    }

    @Test
    public void discardDuringCreationNotificationCannotRemoveItsReplacement() throws Exception {
        CountDownLatch notifying = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        TestCache cache = cache(new TestCache() {
            private final AtomicBoolean first = new AtomicBoolean(true);

            @Override
            protected TestObject whenCreated(TestObject object) {
                if (first.getAndSet(false)) {
                    notifying.countDown();
                    await(proceed);
                }
                return object;
            }
        });
        FutureTask<TestObject> creator = start(() -> cache.ensure("main"));
        await(notifying);
        cache.discard(cache.get("main"));
        TestObject replacement = cache.ensure("main");
        proceed.countDown();

        Throwable failure = Assert.assertThrows(ExecutionException.class, () -> result(creator)).getCause();
        Assert.assertTrue(failure instanceof IllegalStateException);
        Assert.assertEquals("Failed to initialize cached object", failure.getMessage());
        Assert.assertSame(replacement, cache.get("main"));
        Assert.assertEquals(1, cache.created.get(0).discards.get());
        Assert.assertEquals(0, replacement.discards.get());
    }

    @Test
    public void removalDuringValidationDoesNotCloseTheSameObjectTwice() throws Exception {
        CountDownLatch checking = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        TestCache cache = cache(new TestCache() {
            @Override
            protected boolean check(TestObject object) {
                if (!object.valid) {
                    checking.countDown();
                    await(proceed);
                }
                return super.check(object);
            }
        });
        TestObject first = cache.ensure("main");
        first.valid = false;
        FutureTask<TestObject> validator = start(() -> cache.ensure("main"));
        await(checking);
        cache.discard(first);
        TestObject second = cache.ensure("main");
        proceed.countDown();

        Assert.assertSame(second, result(validator));
        Assert.assertEquals(1, first.discards.get());
        Assert.assertEquals(0, second.discards.get());
    }

    @Test
    public void disposalClosesEveryEntryEvenWhenOneCleanupFails() throws Exception {
        TestCache cache = cache(new TestCache() {
            @Override
            protected TestObject whenDiscarded(TestObject object) {
                super.whenDiscarded(object);
                if (object.key.equals("main")) throw new IllegalStateException("cleanup failure");
                return object;
            }
        });
        TestObject first = cache.ensure("main");
        TestObject second = cache.ensure("debug");

        cache.dispose();
        cache.dispose();
        cache.discard(first);

        Assert.assertEquals(1, first.discards.get());
        Assert.assertEquals(1, second.discards.get());
        Assert.assertEquals(0, cache.size());
        Assert.assertNull(cache.get("main"));
        Assert.assertThrows(ProcessCanceledException.class, () -> cache.ensure("main"));
    }

    @Test
    public void disposalRejectsLateCreationAndWakesWaiters() throws Exception {
        CountDownLatch creating = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        TestCache cache = cache(new TestCache() {
            @Override
            protected TestObject create(String key) throws Exception {
                TestObject object = super.create(key);
                creating.countDown();
                await(proceed);
                return object;
            }
        });
        FutureTask<TestObject> creator = start(() -> cache.ensure("main"));
        await(creating);
        FutureTask<TestObject> waiter = start(() -> cache.ensure("main"));
        awaitWaiting(waiter);
        cache.dispose();

        Assert.assertTrue(Assert.assertThrows(ExecutionException.class,
                () -> result(waiter)).getCause() instanceof ProcessCanceledException);
        proceed.countDown();
        Assert.assertTrue(Assert.assertThrows(ExecutionException.class,
                () -> result(creator)).getCause() instanceof ProcessCanceledException);
        Assert.assertEquals(1, cache.created.get(0).discards.get());
        Assert.assertEquals(0, cache.size());
    }

    @Test
    public void disposalRejectsValidationInProgress() throws Exception {
        CountDownLatch checking = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        TestCache cache = cache(new TestCache() {
            @Override
            protected boolean check(TestObject object) {
                checking.countDown();
                await(proceed);
                return true;
            }
        });
        TestObject object = cache.ensure("main");
        FutureTask<TestObject> validator = start(() -> cache.ensure("main"));
        await(checking);
        cache.dispose();
        proceed.countDown();

        Assert.assertTrue(Assert.assertThrows(ExecutionException.class,
                () -> result(validator)).getCause() instanceof ProcessCanceledException);
        Assert.assertEquals(1, object.discards.get());
        Assert.assertEquals(0, cache.size());
    }

    @Test
    public void failedCreationNotificationRemovesAndClosesTheEntry() {
        IllegalStateException failure = new IllegalStateException("notification failure");
        TestCache cache = cache(new TestCache() {
            @Override
            protected TestObject whenCreated(TestObject object) {
                throw failure;
            }
        });

        Assert.assertSame(failure, Assert.assertThrows(IllegalStateException.class,
                () -> cache.ensure("main")));
        Assert.assertEquals(0, cache.size());
        Assert.assertEquals(1, cache.created.get(0).discards.get());
    }

    @Test
    public void recursiveCreationFailsWithoutLeavingAReservation() throws Exception {
        TestCache cache = cache(new TestCache() {
            private boolean first = true;

            @Override
            protected TestObject create(String key) throws Exception {
                if (first) {
                    first = false;
                    return ensure(key);
                }
                return super.create(key);
            }
        });
        FutureTask<TestObject> creator = start(() -> cache.ensure("main"));

        Assert.assertTrue(Assert.assertThrows(ExecutionException.class,
                () -> result(creator)).getCause() instanceof IllegalStateException);
        Assert.assertEquals(0, cache.size());
        Assert.assertNotNull(cache.ensure("main"));
    }

    @Test
    public void interruptedWaiterLeavesTheCreatorRunning() throws Exception {
        CountDownLatch creating = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        TestCache cache = cache(new TestCache() {
            @Override
            protected TestObject create(String key) throws Exception {
                creating.countDown();
                await(proceed);
                return super.create(key);
            }
        });
        FutureTask<TestObject> creator = start(() -> cache.ensure("main"));
        await(creating);
        FutureTask<Boolean> waiter = start(() -> {
            try {
                cache.ensure("main");
                Assert.fail("Expected interruption");
                return false;
            } catch (InterruptedException e) {
                return Thread.currentThread().isInterrupted();
            }
        });
        awaitWaiting(waiter);
        workers.get(waiter).interrupt();
        Assert.assertTrue(result(waiter));
        proceed.countDown();

        Assert.assertSame(result(creator), cache.get("main"));
        Assert.assertEquals(1, cache.created.size());
    }

    private TestCache cache(TestCache cache) {
        caches.add(cache);
        return cache;
    }

    private <T> FutureTask<T> start(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<>(callable);
        Thread worker = new Thread(task, "ObjectCacheBaseTest");
        worker.setDaemon(true);
        workers.put(task, worker);
        worker.start();
        return task;
    }

    private static <T> T result(FutureTask<T> task) throws Exception {
        return task.get(5, TimeUnit.SECONDS);
    }

    private void awaitWaiting(FutureTask<?> task) throws InterruptedException {
        Thread worker = workers.get(task);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (worker.getState() != Thread.State.WAITING && !task.isDone() &&
                deadline - System.nanoTime() > 0) {
            Thread.sleep(1);
        }
        Assert.assertEquals("Worker did not reach the cache wait", Thread.State.WAITING, worker.getState());
    }

    private static void await(CountDownLatch latch) {
        try {
            Assert.assertTrue("Cache operation did not reach its checkpoint", latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static class TestCache extends ObjectCacheBase<String, TestObject, Exception> {
        final List<TestObject> created = new CopyOnWriteArrayList<>();

        private TestCache() {
            super(null);
        }

        @Override
        protected TestObject create(String key) throws Exception {
            TestObject object = new TestObject(key);
            created.add(object);
            return object;
        }

        @Override
        protected String getKey(TestObject object) {
            return object.key;
        }

        @Override
        protected boolean check(TestObject object) {
            return object.valid && object.discards.get() == 0;
        }

        @Override
        protected TestObject whenDiscarded(TestObject object) {
            object.discards.incrementAndGet();
            return object;
        }

        @Override
        protected TestObject whenErrored(Throwable e) throws Exception {
            if (e instanceof Exception exception) throw exception;
            throw new AssertionError(e);
        }
    }

    private static class TestObject {
        private final String key;
        private final AtomicInteger discards = new AtomicInteger();
        private volatile boolean valid = true;

        private TestObject(String key) {
            this.key = key;
        }

        // Cache invalidation must match instances even when resources compare equal.
        @Override
        public boolean equals(Object object) {
            return object instanceof TestObject;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }
}
