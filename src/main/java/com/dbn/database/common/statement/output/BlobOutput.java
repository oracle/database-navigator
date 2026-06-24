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

package com.dbn.database.common.statement.output;

import com.dbn.common.exception.Exceptions;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Types;

@Getter
public class BlobOutput extends OutputContent {
    private static final String CONTENT_NAME = "BLOB content";

    private byte[] value;

    public BlobOutput() {
        this(CONTENT_NAME, MAX_LENGTH);
    }

    public BlobOutput(String contentName, int maxLength) {
        super(contentName, maxLength);
    }

    @Override
    public void registerParameters(CallableStatement statement) throws SQLException {
        statement.registerOutParameter(shifted(1), Types.BLOB);
    }

    @Override
    public void read(CallableStatement statement) throws SQLException {
        Blob blob = statement.getBlob(shifted(1));
        if (blob == null) {
            value = new byte[0];
            return;
        }
        checkLength(blob.length());
        try (InputStream binaryStream = blob.getBinaryStream()) {
            value = read(binaryStream);
        } catch (Exception e) {
            throw Exceptions.toSqlException(e);
        }
    }

    private byte[] read(InputStream inputStream) throws Exception {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(getInitialBufferSize(buffer.length));
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            checkLength((long) outputStream.size() + length);
            outputStream.write(buffer, 0, length);
        }
        return outputStream.toByteArray();
    }
}
