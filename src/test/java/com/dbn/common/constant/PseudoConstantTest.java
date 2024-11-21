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

package com.dbn.common.constant;

import com.dbn.common.util.Unsafe;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 1000 threads initializing (or accessing) a set of 1000 constants, some statically initialized upfront
 * The captures of these threads are evaluated against the final capture in pseudo-constant registry
 */
public class PseudoConstantTest {
    private static final int COUNT = 1000; // number of constants to initialize
    private static final AtomicInteger CURSOR = new AtomicInteger(0);
    private static final List<String> PSEUDO_CONSTANT_IDS = Arrays
            .stream(new Object[COUNT])
            .map(obj -> "PSEUDO_CONSTANT_" + CURSOR.incrementAndGet())
            .collect(Collectors.toList());

    /**
     * Testing concurrency and single instantiation of PseudoConstant
     * (1000 threads concurrently trying to initialize 10 different constants)
     */
    @Test
    public void testConcurrency() throws Exception{
        List<Map<String, PseudoConstant>> threadCaptures = new CopyOnWriteArrayList<>();
        List<Thread> threads = new ArrayList<>();

        // create the threads
        for (int i=0; i<100; i++) {
            threads.add(new Thread(() -> {
                Map<String, PseudoConstant> threadCapture = new HashMap<>();
                threadCaptures.add(threadCapture);
                for (String constantId : PSEUDO_CONSTANT_IDS) {
                    //System.out.println(Thread.currentThread().getName() + ": resolving " + constantId);
                    PseudoConstant testPseudoConstant = constant(constantId);
                    threadCapture.put(constantId, testPseudoConstant);
                }
            }));
        }

        // start the threads
        threads.forEach(thread -> thread.start());
        threads.forEach(thread -> Unsafe.warned(() ->thread.join()));

        // verify the instance uniqueness of the constants
        for (Map<String, PseudoConstant> threadCapture : threadCaptures) {
            Assert.assertEquals(threadCapture.size(), PSEUDO_CONSTANT_IDS.size());
            for (String constantId : PSEUDO_CONSTANT_IDS) {
                PseudoConstant pseudoConstant = threadCapture.get(constantId);
                // assert they are same instance of pseudo constant, not only "equals" one other
                PseudoConstant registryCapture = constant(constantId);
                Assert.assertSame(pseudoConstant, registryCapture);
                switch (constantId) {
                    case "PSEUDO_CONSTANT_0":
                        Assert.assertSame(registryCapture, alternative("PSEUDO_CONSTANT_0"));
                        Assert.assertSame(inner(registryCapture), inner(alternative("PSEUDO_CONSTANT_0")));
                        break;
                    case "PSEUDO_CONSTANT_1":
                        Assert.assertSame(registryCapture, alternative("PSEUDO_CONSTANT_1"));
                        Assert.assertSame(inner(registryCapture), inner(alternative("PSEUDO_CONSTANT_1")));
                        break;
                    case "PSEUDO_CONSTANT_2":
                        Assert.assertSame(registryCapture, alternative("PSEUDO_CONSTANT_2"));
                        Assert.assertSame(inner(registryCapture), inner(alternative("PSEUDO_CONSTANT_2")));
                        break;
                    case "PSEUDO_CONSTANT_4":
                        Assert.assertSame(registryCapture, alternative("PSEUDO_CONSTANT_4"));
                        Assert.assertSame(inner(registryCapture), inner(alternative("PSEUDO_CONSTANT_4")));
                        break;
                    case "PSEUDO_CONSTANT_5":
                        Assert.assertSame(registryCapture, alternative("PSEUDO_CONSTANT_5"));
                        Assert.assertSame(inner(registryCapture), inner(alternative("PSEUDO_CONSTANT_5")));
                        break;
                    case "PSEUDO_CONSTANT_6":
                        Assert.assertSame(registryCapture, alternative("PSEUDO_CONSTANT_6"));
                        Assert.assertSame(inner(registryCapture), inner(alternative("PSEUDO_CONSTANT_6")));
                        break;
                    case "PSEUDO_CONSTANT_7":
                        Assert.assertSame(registryCapture, alternative("PSEUDO_CONSTANT_7"));
                        Assert.assertSame(inner(registryCapture), inner(alternative("PSEUDO_CONSTANT_7")));
                        break;
                    case "PSEUDO_CONSTANT_8":
                        Assert.assertSame(registryCapture, alternative("PSEUDO_CONSTANT_8"));
                        Assert.assertSame(inner(registryCapture), inner(alternative("PSEUDO_CONSTANT_8")));
                        break;
                }
            }
            List<Integer> ordinals = threadCapture.values().stream().map(pseudoConstant -> pseudoConstant.ordinal()).distinct().collect(Collectors.toList());
            Assert.assertEquals(ordinals.size(), PSEUDO_CONSTANT_IDS.size());
        }

        PseudoConstant[] values = PseudoConstant.values(constantClass());
        for (int i = 0, valuesLength = values.length; i < valuesLength; i++) {
            PseudoConstant value = values[i];
            Assert.assertEquals(i, value.ordinal());
        }
    }

    private static PseudoConstant constant(String constantId) {
        return PseudoConstant.get(constantClass(), constantId);
    }

    @SneakyThrows
    private static Class<PseudoConstant> constantClass() {
        return (Class<PseudoConstant>) Class.forName("com.dbn.common.constant.TestPseudoConstant");
    }


    @SneakyThrows
    private static PseudoConstant alternative(String constantId) {
        Class<PseudoConstant> constantClass = constantClass();
        Field[] fields = constantClass.getDeclaredFields();
        for (Field field : fields) {
            Object fieldValue = field.get(constantClass);
            if (constantClass.isAssignableFrom(fieldValue.getClass())) { {
                PseudoConstant constant = (PseudoConstant) fieldValue;
                if (constant.id().equals(constantId)) {
                    return constant;
                }
            }}

        }

        Assert.fail("No constant found for id " + constantId);
        throw new IllegalStateException();
    }

    @SneakyThrows
    private static PseudoConstant inner(PseudoConstant constant) {
        Field inner = constant.getClass().getDeclaredField("inner");
        inner.setAccessible(true);
        return (PseudoConstant) inner.get(constant);
    }

}
