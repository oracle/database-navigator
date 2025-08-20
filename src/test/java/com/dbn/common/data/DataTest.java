/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.common.data;

import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

public class DataTest {

    @Test
    public void testStrings() {
        testRoundTrip(List.of("a", "b", "c"), String.class);
    }

    @Test
    public void testOddStrings() {
        testRoundTrip(List.of("a,1", "b,2", "c,3"), String.class);
    }

    @Test
    public void testOddStrings1() {
        testRoundTrip(Arrays.asList("Value1, value2, value3", null, "", "Line1\nLine2", "Quote: \"Yes\""), String.class);
    }


    @Test
    public void testChars() {
        testRoundTrip(Arrays.asList('a', 'b', 'c', null, 'e', 'f', 'g'), Character.class);
    }

    @Test
    public void testShorts() {
        testRoundTrip(Arrays.asList((short)1, (short)2, (short)3, (short)4, null, (short)9), Short.class);
    }

    @Test
    public void testIntegers() {
        testRoundTrip(Arrays.asList(1, 2, 3, 4, null, 9), Integer.class);
    }

    @Test
    public void testLongs() {
        testRoundTrip(Arrays.asList(1D, 2D, 3D, 4D, null, 9D), Double.class);
    }

    @Test
    public void testDoubles() {
        testRoundTrip(Arrays.asList(1L, 2L, 3L, 4L, null, 9L), Long.class);
    }

    @Test
    public void testBooleans() {
        testRoundTrip(Arrays.asList(true, false, true, true, false, true, null, true), Boolean.class);
    }

    @Test
    public void testBigIntegers() {
        testRoundTrip(Arrays.asList(
                new BigInteger("123456789"),
                new BigInteger("12345678901234"),
                null,
                new BigInteger("9999999999999"),
                new BigInteger("987745542332"),
                new BigInteger("1")
                ), BigInteger.class);
    }

    @Test
    public void testBigDecimals() {
        testRoundTrip(Arrays.asList(
                new BigDecimal("123"),
                new BigDecimal("456"),
                null,
                new BigDecimal("456.789"),
                new BigDecimal("911.00"),
                new BigDecimal("999.123")
                ), BigDecimal.class);
    }


    private static <T> void testRoundTrip(List<T> elements, Class<T> type) {
        String csv = Data.listToCsv(elements);
        System.out.println(csv);
        System.out.println(elements);

        List<T> result = Data.csvToList(csv, type);
        Assert.assertEquals(elements, result);
    }

}