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

package com.dbn.generator.code.java.impl;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JdbcConnectorCodeGeneratorSecurityTest {

    @Test
    public void escapesJavaStringLiteralContent() {
        String payload = "\"; throw new RuntimeException(); //\n\\";

        String escaped = JdbcConnectorCodeGenerator.toJavaStringLiteralContent(payload);

        assertEquals("\\\"; throw new RuntimeException(); //\\n\\\\", escaped);
    }

    @Test
    public void rendersCustomPropertiesAsEscapedSourceLines() {
        Map<String, String> properties = new LinkedHashMap<>();
        properties.put("normal", "value");
        properties.put("bad\", ignored", "\"; throw new RuntimeException(); //");
        properties.put("comma=value", "a,b=c");

        String source = JdbcConnectorCodeGenerator.toPropertiesSource(properties);

        assertEquals(
                "properties.put(\"normal\", \"value\");\n" +
                "properties.put(\"bad\\\", ignored\", \"\\\"; throw new RuntimeException(); //\");\n" +
                "properties.put(\"comma=value\", \"a,b=c\");",
                source);
    }

    @Test
    public void rejectsInvalidDriverClassNames() {
        assertEquals("oracle.jdbc.OracleDriver", JdbcConnectorCodeGenerator.getValidDriverClassName("oracle.jdbc.OracleDriver"));
        assertNull(JdbcConnectorCodeGenerator.getValidDriverClassName("oracle.jdbc.OracleDriver; throw new RuntimeException();"));
        assertNull(JdbcConnectorCodeGenerator.getValidDriverClassName("oracle.jdbc.\"OracleDriver\""));
    }
}
