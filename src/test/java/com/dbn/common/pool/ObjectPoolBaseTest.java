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
import java.util.concurrent.atomic.AtomicInteger;

public class ObjectPoolBaseTest {
    private final List<TestPool> pools = new ArrayList<>();
    private final Map<FutureTask<?>, Thread> workers = new IdentityHashMap<>();

    @After
    public void dispose() throws InterruptedException {
        for (Thread worker : workers.values()) worker.interrupt();
        for (Thread worker : workers.values()) {
            worker.join(TimeUnit.SECONDS.toMillis(5));
            Assert.assertFalse("Pool worker did not stop", worker.isAlive());
        }
        for (TestPool pool : pools) pool.dispose();
    }

    @Test
    public void ignoresDuplicateAndForeignReturns() throws Exception {
        TestPool pool = pool(new TestPool(1));
        TestObject object = pool.acquire(0, TimeUnit.SECONDS);
        Assert.assertNull(pool.acquire(0, TimeUnit.SECONDS));

        pool.release(object);
        pool.release(object);
        pool.release(new TestObject());

        Assert.assertSame(object, pool.acquire(0, TimeUnit.SECONDS));
        Assert.assertNull(pool.acquire(0, TimeUnit.SECONDS));
        Assert.assertEquals(1, pool.size());
        Assert.assertEquals(1, pool.created.size());
    }

    @Test
    public void tracksEqualObjectsByIdentity() throws Exception {
        TestPool pool = pool(new TestPool(2));
        TestObject first = pool.acquire(0, TimeUnit.SECONDS);
        TestObject second = pool.acquire(0, TimeUnit.SECONDS);
        Assert.assertEquals(first, second);
        Assert.assertNotSame(first, second);

        pool.release(first);
        pool.release(second);
        pool.discard(second);
        pool.discard(second);
        pool.discard(new TestObject());

        Assert.assertSame(first, pool.acquire(0, TimeUnit.SECONDS));
        Assert.assertEquals(1, pool.size());
        Assert.assertEquals(2, pool.peakSize());
        Assert.assertEquals(0, first.discards.get());
        Assert.assertEquals(1, second.discards.get());
    }

    @Test
    public void cleanupDoesNotInspectBorrowedObjects() throws Exception {
        TestPool pool = pool(new TestPool(1));
        TestObject object = pool.acquire(0, TimeUnit.SECONDS);

        pool.clean(candidate -> {
            Assert.fail("Cleanup inspected a borrowed object");
            return true;
        });

        Assert.assertEquals(0, object.discards.get());
        pool.release(object);
        pool.clean(candidate -> true);
        Assert.assertEquals(1, object.discards.get());
        Assert.assertEquals(0, pool.size());
    }

    @Test
    public void cleanupClaimsIdleObjectsBeforeEvaluatingPredicate() throws Exception {
        TestPool pool = pool(new TestPool(1));
        TestObject object = pool.acquire(0, TimeUnit.SECONDS);
        pool.release(object);
        CountDownLatch inspecting = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);

        FutureTask<Void> cleaner = start(() -> {
            pool.clean(candidate -> {
                inspecting.countDown();
                await(proceed);
                return true;
            });
            return null;
        });
        await(inspecting);
        FutureTask<TestObject> borrower = start(() -> pool.acquire(30, TimeUnit.SECONDS));
        awaitWaiting(borrower);
        proceed.countDown();

        result(cleaner);
        Assert.assertNotSame(object, result(borrower));
        Assert.assertEquals(1, object.discards.get());
    }

    @Test
    public void failedCleanupReturnsItsClaimedObject() throws Exception {
        TestPool pool = pool(new TestPool(1));
        TestObject object = pool.acquire(0, TimeUnit.SECONDS);
        pool.release(object);
        IllegalStateException failure = new IllegalStateException("predicate failure");

        Assert.assertSame(failure, Assert.assertThrows(IllegalStateException.class,
                () -> pool.clean(candidate -> { throw failure; })));
        Assert.assertSame(object, pool.acquire(0, TimeUnit.SECONDS));
        Assert.assertEquals(0, object.discards.get());
    }

    @Test
    public void droppingAnObjectWakesWaitersToCreateItsReplacement() throws Exception {
        TestPool pool = pool(new TestPool(1));
        TestObject object = pool.acquire(0, TimeUnit.SECONDS);
        FutureTask<TestObject> borrower = start(() -> pool.acquire(30, TimeUnit.SECONDS));
        awaitWaiting(borrower);

        pool.discard(object);

        Assert.assertNotSame(object, result(borrower));
        Assert.assertEquals(1, pool.size());
    }

    @Test
    public void failedCreationWakesWaitersToRetry() throws Exception {
        CountDownLatch creating = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        IllegalStateException failure = new IllegalStateException("creation failure");
        TestPool pool = pool(new TestPool(1) {
            private final AtomicInteger attempts = new AtomicInteger();

            @Override
            protected TestObject create() {
                if (attempts.incrementAndGet() == 1) {
                    creating.countDown();
                    await(proceed);
                    throw failure;
                }
                return super.create();
            }
        });
        FutureTask<TestObject> first = start(() -> pool.acquire(30, TimeUnit.SECONDS));
        await(creating);
        FutureTask<TestObject> second = start(() -> pool.acquire(30, TimeUnit.SECONDS));
        awaitWaiting(second);
        proceed.countDown();

        Assert.assertSame(failure, Assert.assertThrows(ExecutionException.class,
                () -> result(first)).getCause());
        Assert.assertNotNull(result(second));
        Assert.assertEquals(1, pool.size());
    }

    @Test
    public void concurrentCreationRespectsCapacity() throws Exception {
        CountDownLatch creating = new CountDownLatch(2);
        CountDownLatch proceed = new CountDownLatch(1);
        TestPool pool = pool(new TestPool(2) {
            @Override
            protected TestObject create() {
                creating.countDown();
                await(proceed);
                return super.create();
            }
        });
        FutureTask<TestObject> first = start(() -> pool.acquire(30, TimeUnit.SECONDS));
        FutureTask<TestObject> second = start(() -> pool.acquire(30, TimeUnit.SECONDS));
        await(creating);
        FutureTask<TestObject> third = start(() -> pool.acquire(30, TimeUnit.SECONDS));
        awaitWaiting(third);
        Assert.assertEquals(2, pool.size());
        proceed.countDown();

        TestObject object = result(first);
        Assert.assertNotSame(object, result(second));
        Assert.assertFalse(third.isDone());
        pool.release(object);
        Assert.assertSame(object, result(third));
        Assert.assertEquals(2, pool.created.size());
        Assert.assertEquals(2, pool.size());
        Assert.assertEquals(2, pool.peakSize());
    }

    @Test
    public void initializationCompletesBeforePublication() throws Exception {
        CountDownLatch initializing = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        TestPool pool = pool(new TestPool(1) {
            @Override
            protected TestObject whenCreated(TestObject object) {
                initializing.countDown();
                await(proceed);
                return object;
            }
        });
        FutureTask<TestObject> first = start(() -> pool.acquire(30, TimeUnit.SECONDS));
        await(initializing);
        FutureTask<TestObject> second = start(() -> pool.acquire(30, TimeUnit.SECONDS));
        awaitWaiting(second);
        proceed.countDown();

        TestObject object = result(first);
        Assert.assertFalse(second.isDone());
        pool.release(object);
        Assert.assertSame(object, result(second));
    }

    @Test
    public void disposalDiscardsIdleAndBorrowedObjectsExactlyOnce() throws Exception {
        TestPool pool = pool(new TestPool(2));
        TestObject first = pool.acquire(0, TimeUnit.SECONDS);
        TestObject second = pool.acquire(0, TimeUnit.SECONDS);
        pool.release(first);

        pool.dispose();
        pool.dispose();
        pool.release(second);
        pool.discard(first);

        Assert.assertEquals(1, first.discards.get());
        Assert.assertEquals(1, second.discards.get());
        Assert.assertEquals(0, pool.size());
        Assert.assertThrows(ProcessCanceledException.class,
                () -> pool.acquire(0, TimeUnit.SECONDS));
    }

    @Test
    public void disposalRejectsCreationInProgressAndWakesWaiters() throws Exception {
        CountDownLatch initializing = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        TestPool pool = pool(new TestPool(1) {
            @Override
            protected TestObject whenCreated(TestObject object) {
                initializing.countDown();
                await(proceed);
                return object;
            }
        });
        FutureTask<TestObject> creator = start(() -> pool.acquire(30, TimeUnit.SECONDS));
        await(initializing);
        FutureTask<TestObject> waiter = start(() -> pool.acquire(30, TimeUnit.SECONDS));
        awaitWaiting(waiter);

        pool.dispose();
        Assert.assertTrue(Assert.assertThrows(ExecutionException.class,
                () -> result(waiter)).getCause() instanceof ProcessCanceledException);
        proceed.countDown();
        Assert.assertTrue(Assert.assertThrows(ExecutionException.class,
                () -> result(creator)).getCause() instanceof ProcessCanceledException);
        Assert.assertEquals(1, pool.created.get(0).discards.get());
        Assert.assertEquals(0, pool.size());
    }

    @Test
    public void disposalRejectsAcquisitionInProgress() throws Exception {
        CountDownLatch acquiring = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        TestPool pool = pool(new TestPool(1) {
            @Override
            protected TestObject whenAcquired(TestObject object) {
                acquiring.countDown();
                await(proceed);
                return object;
            }
        });
        FutureTask<TestObject> borrower = start(() -> pool.acquire(30, TimeUnit.SECONDS));
        await(acquiring);
        pool.dispose();
        proceed.countDown();

        Assert.assertTrue(Assert.assertThrows(ExecutionException.class,
                () -> result(borrower)).getCause() instanceof ProcessCanceledException);
        Assert.assertEquals(1, pool.created.get(0).discards.get());
        Assert.assertEquals(0, pool.size());
    }

    @Test
    public void releaseCannotReviveDiscardedObjects() throws Exception {
        assertReleaseCannotRevive(false);
    }

    @Test
    public void releaseCannotReviveDisposedObjects() throws Exception {
        assertReleaseCannotRevive(true);
    }

    private void assertReleaseCannotRevive(boolean dispose) throws Exception {
        CountDownLatch releasing = new CountDownLatch(1);
        CountDownLatch proceed = new CountDownLatch(1);
        TestPool pool = pool(new TestPool(1) {
            @Override
            protected TestObject whenReleased(TestObject object) {
                releasing.countDown();
                await(proceed);
                return object;
            }
        });
        TestObject object = pool.acquire(0, TimeUnit.SECONDS);
        FutureTask<TestObject> release = start(() -> pool.release(object));
        await(releasing);
        if (dispose) pool.dispose(); else pool.discard(object);
        proceed.countDown();
        result(release);

        Assert.assertEquals(1, object.discards.get());
        Assert.assertEquals(0, pool.size());
        if (!dispose) Assert.assertNotSame(object, pool.acquire(0, TimeUnit.SECONDS));
    }

    @Test
    public void failedInitializationDestroysTheCreatedObject() {
        IllegalStateException failure = new IllegalStateException("initialization failure");
        TestPool pool = pool(new TestPool(1) {
            @Override
            protected TestObject whenCreated(TestObject object) {
                throw failure;
            }
        });

        Assert.assertSame(failure, Assert.assertThrows(IllegalStateException.class,
                () -> pool.acquire(0, TimeUnit.SECONDS)));
        Assert.assertEquals(0, pool.size());
        Assert.assertEquals(1, pool.created.get(0).discards.get());
    }

    @Test
    public void failedValidationDoesNotConsumeCapacity() throws Exception {
        IllegalStateException failure = new IllegalStateException("validation failure");
        TestPool pool = pool(new TestPool(1) {
            private boolean failed;

            @Override
            protected boolean check(TestObject object) {
                if (!failed) {
                    failed = true;
                    throw failure;
                }
                return super.check(object);
            }
        });

        Assert.assertSame(failure, Assert.assertThrows(IllegalStateException.class,
                () -> pool.acquire(0, TimeUnit.SECONDS)));
        Assert.assertEquals(0, pool.size());
        Assert.assertEquals(1, pool.created.get(0).discards.get());
        Assert.assertNotNull(pool.acquire(0, TimeUnit.SECONDS));
    }

    @Test
    public void failedAcquisitionDoesNotConsumeCapacity() {
        IllegalStateException failure = new IllegalStateException("acquisition failure");
        TestPool pool = pool(new TestPool(1) {
            @Override
            protected TestObject whenAcquired(TestObject object) {
                throw failure;
            }
        });

        Assert.assertSame(failure, Assert.assertThrows(IllegalStateException.class,
                () -> pool.acquire(0, TimeUnit.SECONDS)));
        Assert.assertEquals(0, pool.size());
        Assert.assertEquals(1, pool.created.get(0).discards.get());
    }

    @Test
    public void failedReleaseDiscardsTheObject() throws Exception {
        TestPool pool = pool(new TestPool(1) {
            @Override
            protected TestObject whenReleased(TestObject object) {
                throw new IllegalStateException("release failure");
            }
        });
        TestObject object = pool.acquire(0, TimeUnit.SECONDS);
        pool.release(object);

        Assert.assertEquals(1, object.discards.get());
        Assert.assertEquals(0, pool.size());
        Assert.assertNotSame(object, pool.acquire(0, TimeUnit.SECONDS));
    }

    @Test
    public void invalidObjectsShareOneRetryDeadline() throws Exception {
        TestPool pool = pool(new TestPool(1) {
            @Override
            protected boolean check(TestObject object) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError(e);
                }
                return false;
            }
        });
        FutureTask<TestObject> acquire = start(() -> pool.acquire(5, TimeUnit.MILLISECONDS));

        Assert.assertNull(result(acquire));
        Assert.assertEquals(1, pool.created.size());
        Assert.assertEquals(1, pool.created.get(0).discards.get());
        Assert.assertEquals(0, pool.size());
    }

    @Test
    public void interruptionRestoresTheInterruptFlag() throws Exception {
        TestPool pool = pool(new TestPool(1));
        pool.acquire(0, TimeUnit.SECONDS);
        FutureTask<Boolean> waiter = start(() -> {
            try {
                pool.acquire(30, TimeUnit.SECONDS);
                Assert.fail("Expected interruption");
                return false;
            } catch (InterruptedException e) {
                return Thread.currentThread().isInterrupted();
            }
        });
        awaitWaiting(waiter);
        workers.get(waiter).interrupt();

        Assert.assertTrue(result(waiter));
        Assert.assertEquals(1, pool.size());
    }

    @Test
    public void cancellationIsPropagatedAfterCleanup() {
        ProcessCanceledException cancelled = new ProcessCanceledException();
        TestPool pool = pool(new TestPool(1) {
            @Override
            protected TestObject whenAcquired(TestObject object) {
                throw cancelled;
            }
        });

        Assert.assertSame(cancelled, Assert.assertThrows(ProcessCanceledException.class,
                () -> pool.acquire(0, TimeUnit.SECONDS)));
        Assert.assertEquals(0, pool.size());
        Assert.assertEquals(1, pool.created.get(0).discards.get());
    }

    @Test
    public void errorsArePropagatedAfterCleanup() {
        AssertionError failure = new AssertionError("acquisition failure");
        TestPool pool = pool(new TestPool(1) {
            @Override
            protected TestObject whenAcquired(TestObject object) {
                throw failure;
            }
        });

        Assert.assertSame(failure, Assert.assertThrows(AssertionError.class,
                () -> pool.acquire(0, TimeUnit.SECONDS)));
        Assert.assertEquals(0, pool.size());
        Assert.assertEquals(1, pool.created.get(0).discards.get());
    }

    private TestPool pool(TestPool pool) {
        pools.add(pool);
        return pool;
    }

    private <T> FutureTask<T> start(Callable<T> callable) {
        FutureTask<T> task = new FutureTask<>(callable);
        Thread worker = new Thread(task, "ObjectPoolBaseTest");
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
        while (worker.getState() != Thread.State.TIMED_WAITING && !task.isDone() &&
                deadline - System.nanoTime() > 0) {
            Thread.sleep(1);
        }
        Assert.assertEquals("Worker did not reach the pool wait", Thread.State.TIMED_WAITING, worker.getState());
    }

    private static void await(CountDownLatch latch) {
        try {
            Assert.assertTrue("Pool operation did not reach its checkpoint", latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    private static class TestPool extends ObjectPoolBase<TestObject, Exception> {
        final List<TestObject> created = new CopyOnWriteArrayList<>();
        private final int maximum;

        private TestPool(int maximum) {
            super(null);
            this.maximum = maximum;
        }

        @Override
        public int maxSize() {
            return maximum;
        }

        @Override
        protected TestObject create() {
            TestObject object = new TestObject();
            created.add(object);
            return object;
        }

        @Override
        protected boolean check(TestObject object) {
            return object.discards.get() == 0;
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
        private final AtomicInteger discards = new AtomicInteger();

        // Distinct resources may compare equal; pool ownership must still use identity.
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
