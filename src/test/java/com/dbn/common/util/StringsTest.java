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

import junit.framework.TestCase;

public class StringsTest extends TestCase {

    public void testTrim1() {
        StringBuilder builder = new StringBuilder();

        Strings.trim(builder);
        assertEquals("", builder.toString());
    }

    public void testTrim2() {
        StringBuilder builder = new StringBuilder("\n\t\n  \n   \n\t  ");

        Strings.trim(builder);
        assertEquals("", builder.toString());
    }

    public void testTrim3() {
        StringBuilder builder = new StringBuilder("test \n   \n\t");

        Strings.trim(builder);
        assertEquals("test", builder.toString());
    }

    public void testTrim4() {
        StringBuilder builder = new StringBuilder("\n\t\n test");

        Strings.trim(builder);
        assertEquals("test", builder.toString());
    }

    public void testTrim5() {
        StringBuilder builder = new StringBuilder("\n\t\n test \n   \n\t");

        Strings.trim(builder);
        assertEquals("test", builder.toString());
    }

}