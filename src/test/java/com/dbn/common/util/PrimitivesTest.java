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

package com.dbn.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class PrimitivesTest {

    @Test
    public void testGetBoxedClassForPrimitive() {
        assertEquals(Integer.class, Primitives.getBoxedClass(int.class));
        assertEquals(Boolean.class, Primitives.getBoxedClass(boolean.class));
        assertEquals(Character.class, Primitives.getBoxedClass(char.class));
        assertEquals(Double.class, Primitives.getBoxedClass(double.class));
        assertEquals(Float.class, Primitives.getBoxedClass(float.class));
        assertEquals(Long.class, Primitives.getBoxedClass(long.class));
        assertEquals(Short.class, Primitives.getBoxedClass(short.class));
        assertEquals(Byte.class, Primitives.getBoxedClass(byte.class));
        assertEquals(Void.class, Primitives.getBoxedClass(void.class));
    }

    @Test
    public void testGetBoxedClassForBoxed() {
        assertEquals(Integer.class, Primitives.getBoxedClass(Integer.class));
        assertEquals(Boolean.class, Primitives.getBoxedClass(Boolean.class));
        assertEquals(Character.class, Primitives.getBoxedClass(Character.class));
        assertEquals(Double.class, Primitives.getBoxedClass(Double.class));
        assertEquals(Float.class, Primitives.getBoxedClass(Float.class));
        assertEquals(Long.class, Primitives.getBoxedClass(Long.class));
        assertEquals(Short.class, Primitives.getBoxedClass(Short.class));
        assertEquals(Byte.class, Primitives.getBoxedClass(Byte.class));
        assertEquals(Void.class, Primitives.getBoxedClass(Void.class));
    }

    @Test
    public void testGetBoxedClassForNonPrimitive() {
        assertNull(Primitives.getBoxedClass(String.class));
        assertNull(Primitives.getBoxedClass(Object.class));
    }

    @Test
    public void testGetPrimitiveClassForPrimitive() {
        assertEquals(int.class, Primitives.getPrimitiveClass(int.class));
        assertEquals(boolean.class, Primitives.getPrimitiveClass(boolean.class));
        assertEquals(char.class, Primitives.getPrimitiveClass(char.class));
        assertEquals(double.class, Primitives.getPrimitiveClass(double.class));
        assertEquals(float.class, Primitives.getPrimitiveClass(float.class));
        assertEquals(long.class, Primitives.getPrimitiveClass(long.class));
        assertEquals(short.class, Primitives.getPrimitiveClass(short.class));
        assertEquals(byte.class, Primitives.getPrimitiveClass(byte.class));
        assertEquals(void.class, Primitives.getPrimitiveClass(void.class));
    }

    @Test
    public void testGetPrimitiveClassForBoxed() {
        assertEquals(int.class, Primitives.getPrimitiveClass(Integer.class));
        assertEquals(boolean.class, Primitives.getPrimitiveClass(Boolean.class));
        assertEquals(char.class, Primitives.getPrimitiveClass(Character.class));
        assertEquals(double.class, Primitives.getPrimitiveClass(Double.class));
        assertEquals(float.class, Primitives.getPrimitiveClass(Float.class));
        assertEquals(long.class, Primitives.getPrimitiveClass(Long.class));
        assertEquals(short.class, Primitives.getPrimitiveClass(Short.class));
        assertEquals(byte.class, Primitives.getPrimitiveClass(Byte.class));
        assertEquals(void.class, Primitives.getPrimitiveClass(Void.class));
    }

    @Test
    public void testGetPrimitiveClassForNonBoxed() {
        assertNull(Primitives.getPrimitiveClass(String.class));
        assertNull(Primitives.getPrimitiveClass(Object.class));
    }

    @Test
    public void testIsPrimitive() {
        assertTrue(Primitives.isPrimitive(int.class));
        assertTrue(Primitives.isPrimitive(boolean.class));
        assertTrue(Primitives.isPrimitive(double.class));
        assertTrue(Primitives.isPrimitive(float.class));
        assertTrue(Primitives.isPrimitive(long.class));
        assertTrue(Primitives.isPrimitive(short.class));
        assertTrue(Primitives.isPrimitive(byte.class));
        assertTrue(Primitives.isPrimitive(char.class));
        assertTrue(Primitives.isPrimitive(void.class));

        assertFalse(Primitives.isPrimitive(Integer.class));
        assertFalse(Primitives.isPrimitive(String.class));
        assertFalse(Primitives.isPrimitive(Object.class));
    }

    @Test
    public void testIsBoxed() {
        assertTrue(Primitives.isBoxed(Integer.class));
        assertTrue(Primitives.isBoxed(Boolean.class));
        assertTrue(Primitives.isBoxed(Double.class));
        assertTrue(Primitives.isBoxed(Float.class));
        assertTrue(Primitives.isBoxed(Long.class));
        assertTrue(Primitives.isBoxed(Short.class));
        assertTrue(Primitives.isBoxed(Byte.class));
        assertTrue(Primitives.isBoxed(Character.class));
        assertTrue(Primitives.isBoxed(Void.class));

        assertFalse(Primitives.isBoxed(int.class));
        assertFalse(Primitives.isBoxed(String.class));
        assertFalse(Primitives.isBoxed(Object.class));
    }

    @Test
    public void testAreEquivalent() {
        assertTrue(Primitives.areEquivalent(int.class, Integer.class));
        assertTrue(Primitives.areEquivalent(Integer.class, int.class));
        assertTrue(Primitives.areEquivalent(double.class, Double.class));
        assertTrue(Primitives.areEquivalent(Double.class, double.class));
        assertTrue(Primitives.areEquivalent(void.class, Void.class));
        assertTrue(Primitives.areEquivalent(Void.class, void.class));

        assertFalse(Primitives.areEquivalent(int.class, String.class));
        assertFalse(Primitives.areEquivalent(String.class, Object.class));
        assertFalse(Primitives.areEquivalent(int.class, double.class));
        assertFalse(Primitives.areEquivalent(Integer.class, Double.class));
    }

}