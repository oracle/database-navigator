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

import com.dbn.common.Priority;
import com.dbn.common.exception.Exceptions;
import com.dbn.common.thread.Threads;
import com.dbn.common.util.TimeUtil;
import com.dbn.common.util.Unsafe;
import lombok.AllArgsConstructor;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Consumer;

public class DatabaseInterfaceQueueTest {
    private static final Random random = new Random();
    private InterfaceQueue queue;

    @Before
    public void setUp() throws Exception {
        queue = new InterfaceQueue(null, task -> Threads.backgroundExecutor().submit(() -> queue.executeTask(task)));
    }

    @Test
    public void scheduleAndWait() {
        invoke(100, task -> {
            try {
                InterfaceTaskRequest taskDefinition = InterfaceTaskRequest.create(task.priority, "test", "test", null, null, null);
                queue.scheduleAndWait(taskDefinition, () -> {
                    System.out.println("Executing " + task);
                    Unsafe.silent(() -> Thread.sleep(task.time));
                    System.out.println("Completed "  + task);
                });

                System.out.println("Finished executing " + task);
            } catch (SQLException e) {
                throw Exceptions.toRuntimeException(e);
            }
        });
    }

    @Test
    public void scheduleAndForget() {
        invoke(100, task -> {
            try {
                InterfaceTaskRequest taskDefinition = InterfaceTaskRequest.create(task.priority, "test", "test", null, null, null);
                queue.scheduleAndForget(taskDefinition, () -> {
                    System.out.println("Executing " + task);
                    Unsafe.silent(() -> Thread.sleep(task.time));
                    System.out.println("Completed "  + task);
                });
            } catch (SQLException e) {
                throw Exceptions.toRuntimeException(e);
            }
        });
    }



    private void invoke(int times, Consumer<TestTask> runnable) {
        long start = System.currentTimeMillis();
        AtomicLong totalTime = new AtomicLong();

        ExecutorService executorService = Executors.newCachedThreadPool();
        for (int i = 0; i < times; i++) {
            int index = i;
            System.out.println("Queueing " + index);
            executorService.submit(() -> {
                TestTask task = new TestTask(index);
                totalTime.addAndGet(task.time);
                runnable.accept(task);
            });
        }

        Thread invoker = Thread.currentThread();
        queue.counters().running().addListener(value -> {
            if (value == 0 && queue.size() == 0) {
                LockSupport.unpark(invoker);
                System.out.println("UNPARKED");
            }
        });


        long sleepStart = System.currentTimeMillis();
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(120));
        System.out.println("DONE " + TimeUnit.MILLISECONDS.toSeconds(TimeUtil.millisSince(sleepStart)));

        executorService.shutdown();
        Assert.assertEquals(times, queue.counters().finished().get());
        long elapsedTime = TimeUtil.millisSince(start);
        long activeTime = totalTime.get() / queue.maxActiveTasks();
        long difference = Math.abs(activeTime - elapsedTime);

        System.out.println("Execution time " + activeTime);
        System.out.println("Elapsed time " + elapsedTime);
        System.out.println("Difference " + difference);

        Assert.assertTrue(difference < 500);
    }

    @AllArgsConstructor
    private static class TestTask {
        private final int index;
        private final long time = random.nextInt(500);
        private final Priority priority = randomEnum(Priority.class);

        @Override
        public String toString() {
            return "Task " + index + " (" + priority + " - " + time + "ms)";
        }
    }


    public static <T extends Enum<?>> T randomEnum(Class<T> clazz){
        T[] enumConstants = clazz.getEnumConstants();
        int index = random.nextInt(enumConstants.length);
        return enumConstants[index];
    }
}