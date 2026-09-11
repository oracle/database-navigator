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

package com.dbn.data.value;

import org.junit.Test;

import java.io.StringReader;
import java.lang.reflect.Proxy;
import java.sql.ResultSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class JsonValueTest {
    @Test
    public void readsJsonThroughBoundedCharacterStream() throws Exception {
        String content = new String(new char[LargeObjectValue.MAX_READ_SIZE + 1]);
        JsonValue value = new JsonValue(resultSetWith(content), 1);

        assertEquals(LargeObjectValue.MAX_READ_SIZE, value.getData().length());
        assertTrue(value.isTruncated());
    }

    @Test
    public void doesNotUseUnboundedStringConversion() throws Exception {
        ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(
                JsonValueTest.class.getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("getCharacterStream")) return new StringReader("{}");
                    if (method.getName().equals("getString")) throw new AssertionError("getString must not be used");
                    return null;
                });

        JsonValue value = new JsonValue(resultSet, 1);

        assertEquals("{}", value.getData());
        assertFalse(value.isTruncated());
    }

    private static ResultSet resultSetWith(String content) {
        return (ResultSet) Proxy.newProxyInstance(
                JsonValueTest.class.getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, arguments) -> method.getName().equals("getCharacterStream") ? new StringReader(content) : null);
    }
}
