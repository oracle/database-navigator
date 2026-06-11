/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.execution.java;

import org.junit.Test;

import static com.dbn.execution.java.JavaExecutionTypeResolver.isInputTypeSupported;
import static com.dbn.execution.java.JavaExecutionTypeResolver.resolveInputType;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class JavaExecutionTypeResolverTest {

    @Test
    public void resolvesSafeInputTypes() {
        assertSame(int.class, resolveInputType("int"));
        assertSame(String.class, resolveInputType("java.lang.String"));
    }

    @Test
    public void rejectsDangerousClasspathTypes() {
        assertFalse(isInputTypeSupported("java.lang.Runtime"));
        assertUnsupported("java.lang.Runtime");
    }

    @Test
    public void rejectsUnknownTypesWithClearMessage() {
        assertUnsupported("com.example.DoesNotExist");
    }

    private static void assertUnsupported(String className) {
        try {
            resolveInputType(className);
            fail("Expected unsupported Java execution type to be rejected");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unsupported Java execution parameter type: " + className));
        }
    }
}
