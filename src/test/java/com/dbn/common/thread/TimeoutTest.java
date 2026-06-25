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

package com.dbn.common.thread;

import com.intellij.openapi.progress.ProcessCanceledException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class TimeoutTest {

    @Test
    public void callReturnsCallableResult() {
        String result = Timeout.call("test-call", 1, "default", true, () -> "result");

        Assert.assertEquals("result", result);
    }

    @Test
    public void callPropagatesCallableException() {
        IOException expected = new IOException("expected");

        IOException actual = Assert.assertThrows(IOException.class,
                () -> Timeout.call("test-call", 1, null, true, () -> {
                    throw expected;
                }));

        Assert.assertSame(expected, actual);
    }

    @Test
    public void runExecutesRunnable() {
        AtomicInteger counter = new AtomicInteger();

        Timeout.run(1, true, counter::incrementAndGet);

        Assert.assertEquals(1, counter.get());
    }

    @Test
    public void runPropagatesRunnableException() {
        IOException expected = new IOException("expected");

        IOException actual = Assert.assertThrows(IOException.class,
                () -> Timeout.run(1, true, () -> {
                    throw expected;
                }));

        Assert.assertSame(expected, actual);
    }

    @Test(timeout = 3000)
    public void callReturnsDefaultValueOnTimeout() throws InterruptedException {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch interrupted = new CountDownLatch(1);
        AtomicBoolean interruptFlag = new AtomicBoolean();

        String result = Timeout.call("test-call", 1, "default", true, () -> {
            started.countDown();
            try {
                Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            } catch (InterruptedException e) {
                interruptFlag.set(true);
                interrupted.countDown();
            }
            return "result";
        });

        Assert.assertTrue(started.await(1, TimeUnit.SECONDS));
        Assert.assertEquals("default", result);
        Assert.assertTrue(interrupted.await(1, TimeUnit.SECONDS));
        Assert.assertTrue(interruptFlag.get());
    }

    @Test(timeout = 3000)
    public void callPropagatesCallerInterruptionAsCancellation() throws InterruptedException {
        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch waiterStarted = new CountDownLatch(1);
        AtomicBoolean cancelled = new AtomicBoolean();
        AtomicBoolean interruptFlagRestored = new AtomicBoolean();

        Thread waiter = new Thread(() -> {
            waiterStarted.countDown();
            try {
                Timeout.call("test-call", 10, "default", true, () -> {
                    workerStarted.countDown();
                    Thread.sleep(TimeUnit.SECONDS.toMillis(10));
                    return "result";
                });
                Assert.fail("Expected cancellation");
            } catch (ProcessCanceledException e) {
                cancelled.set(true);
                interruptFlagRestored.set(Thread.currentThread().isInterrupted());
            }
        });

        waiter.start();
        Assert.assertTrue(waiterStarted.await(1, TimeUnit.SECONDS));
        Assert.assertTrue(workerStarted.await(1, TimeUnit.SECONDS));

        waiter.interrupt();
        waiter.join(TimeUnit.SECONDS.toMillis(2));

        Assert.assertFalse(waiter.isAlive());
        Assert.assertTrue(cancelled.get());
        Assert.assertTrue(interruptFlagRestored.get());
    }

    @Test
    public void waitForReturnsFutureResult() throws Exception {
        FutureTask<String> future = new FutureTask<>(() -> "result");
        new Thread(future).start();

        String result = Timeout.waitFor(future, 1, TimeUnit.SECONDS);

        Assert.assertEquals("result", result);
    }

    @Test(timeout = 3000)
    public void waitForCancelsFutureOnTimeout() {
        FutureTask<String> future = new FutureTask<>(() -> {
            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            return "result";
        });
        new Thread(future).start();

        Assert.assertThrows(TimeoutException.class,
                () -> Timeout.waitFor(future, 1, TimeUnit.MILLISECONDS));

        Assert.assertTrue(future.isCancelled());
    }

    @Test(timeout = 3000)
    public void waitForCancelsFutureOnInterruption() throws InterruptedException {
        CountDownLatch futureStarted = new CountDownLatch(1);
        CountDownLatch waiterStarted = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean();
        AtomicBoolean futureCancelled = new AtomicBoolean();

        FutureTask<String> future = new FutureTask<>(() -> {
            futureStarted.countDown();
            Thread.sleep(TimeUnit.SECONDS.toMillis(10));
            return "result";
        });
        new Thread(future).start();

        Thread waiter = new Thread(() -> {
            waiterStarted.countDown();
            try {
                Timeout.waitFor(future, 10, TimeUnit.SECONDS);
                Assert.fail("Expected interruption");
            } catch (InterruptedException e) {
                interrupted.set(true);
                futureCancelled.set(future.isCancelled());
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });

        waiter.start();
        Assert.assertTrue(futureStarted.await(1, TimeUnit.SECONDS));
        Assert.assertTrue(waiterStarted.await(1, TimeUnit.SECONDS));

        waiter.interrupt();
        waiter.join(TimeUnit.SECONDS.toMillis(2));

        Assert.assertFalse(waiter.isAlive());
        Assert.assertTrue(interrupted.get());
        Assert.assertTrue(futureCancelled.get());
    }
}
