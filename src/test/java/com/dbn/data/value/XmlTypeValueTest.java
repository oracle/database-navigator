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

import com.dbn.database.oracle.jdbc.OracleXmlType;
import org.junit.Test;

import javax.sql.rowset.serial.SerialClob;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class XmlTypeValueTest {
    @Test
    public void readsXmlTypeThroughBoundedClob() throws Exception {
        String content = "x".repeat(LargeObjectValue.MAX_READ_SIZE + 1);
        XmlTypeValue value = valueWithProxy(xmlType((proxy, method, arguments) -> switch (method.getName()) {
            case "getClobVal" -> new SerialClob(content.toCharArray());
            case "getInputStream", "getStringVal" -> throw new AssertionError("Unexpected fallback");
            default -> null;
        }));

        String result = value.read();

        assertEquals(LargeObjectValue.MAX_READ_SIZE, result.length());
        assertTrue(value.isTruncated());
    }

    @Test
    public void fallsBackFromClobToInputStream() throws Exception {
        String content = "<root>stream</root>";
        XmlTypeValue value = valueWithProxy(xmlType((proxy, method, arguments) -> switch (method.getName()) {
            case "getClobVal" -> throw new SQLException("CLOB unavailable");
            case "getInputStream" -> new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            case "getStringVal" -> throw new AssertionError("Unexpected fallback");
            default -> null;
        }));

        assertEquals(content, value.read());
    }

    @Test
    public void fallsBackFromStreamToString() throws Exception {
        String content = "<root>string</root>";
        XmlTypeValue value = valueWithProxy(xmlType((proxy, method, arguments) -> switch (method.getName()) {
            case "getClobVal", "getInputStream" -> throw new SQLException("Reader unavailable");
            case "getStringVal" -> content;
            default -> null;
        }));

        assertEquals(content, value.read());
    }

    @Test
    public void returnsNullForNullXmlTypeInputs() throws Exception {
        assertNull(OracleXmlType.createXML((Object) null));
        assertNull(OracleXmlType.createXML((AutoCloseable) null, null, null));
    }

    @Test
    public void bindsNullPreparedXmlType() throws Exception {
        Object unset = new Object();
        AtomicBoolean called = new AtomicBoolean();
        AtomicReference<Object> boundValue = new AtomicReference<>(unset);
        PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(
                XmlTypeValueTest.class.getClassLoader(),
                new Class[]{PreparedStatement.class},
                (proxy, method, arguments) -> {
                    if (method.getName().equals("setObject")) {
                        called.set(true);
                        boundValue.set(arguments[1]);
                    }
                    return null;
                });

        new XmlTypeValue().write(null, statement, 1, null);

        assertTrue(called.get());
        assertNull(boundValue.get());
    }

    private static XmlTypeValue valueWithProxy(OracleXmlType proxy) throws Exception {
        XmlTypeValue value = new XmlTypeValue();
        Field field = XmlTypeValue.class.getDeclaredField("xmlType");
        field.setAccessible(true);
        field.set(value, proxy);
        return value;
    }

    private static OracleXmlType xmlType(InvocationHandler handler) {
        return (OracleXmlType) Proxy.newProxyInstance(
                XmlTypeValueTest.class.getClassLoader(),
                new Class[]{OracleXmlType.class},
                handler);
    }

    @FunctionalInterface
    private interface InvocationHandler extends java.lang.reflect.InvocationHandler {
    }
}
