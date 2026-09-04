/*
 * Copyright 2026 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.data.value;

import org.junit.Test;

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.lang.reflect.Proxy;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class LargeObjectValueTest {
    @Test
    public void limitsExplicitClobReadsToMaximumSize() throws Exception {
        ClobValue value = new ClobValue(resultSetWith(new SerialClob(new char[LargeObjectValue.MAX_READ_SIZE + 1])), 1);

        String content = value.read(0);

        assertEquals(LargeObjectValue.MAX_READ_SIZE, content.length());
        assertTrue(value.isTruncated());
    }

    @Test
    public void rejectsUnboundedClobReadsAboveMaximumSize() throws Exception {
        ClobValue value = new ClobValue(resultSetWith(new SerialClob(new char[LargeObjectValue.MAX_READ_SIZE + 1])), 1);

        assertThrows(SQLException.class, value::read);
        assertTrue(value.isTruncated());
    }

    @Test
    public void rejectsUnboundedBlobReadsAboveMaximumSize() throws Exception {
        BlobValue value = new BlobValue(resultSetWith(new SerialBlob(new byte[LargeObjectValue.MAX_READ_SIZE + 1])), 1);

        assertThrows(SQLException.class, value::read);
        assertTrue(value.isTruncated());
    }

    private static ResultSet resultSetWith(Clob clob) {
        return (ResultSet) Proxy.newProxyInstance(
                LargeObjectValueTest.class.getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, arguments) -> method.getName().equals("getClob") ? clob : null);
    }

    private static ResultSet resultSetWith(Blob blob) {
        return (ResultSet) Proxy.newProxyInstance(
                LargeObjectValueTest.class.getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, arguments) -> method.getName().equals("getBlob") ? blob : null);
    }
}
