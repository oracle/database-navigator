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

import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.Reader;
import java.sql.SQLException;

@Getter
@Setter
public abstract class LargeObjectValue extends ValueAdapter<String> {
    public static final int MAX_READ_SIZE = 1024 * 1024;

    private boolean truncated;

    public abstract String read(int maxSize) throws SQLException;
    public abstract long size() throws SQLException;
    public abstract void release();

    protected final String readCharacterStream(Reader reader, int maxSize) throws SQLException {
        if (reader == null) {
            setTruncated(false);
            return null;
        }

        int size = maxSize <= 0 ? MAX_READ_SIZE : Math.min(maxSize, MAX_READ_SIZE);
        char[] buffer = new char[size + 1];
        int length = 0;
        try (Reader valueReader = reader) {
            while (length < buffer.length) {
                int count = valueReader.read(buffer, length, buffer.length - length);
                if (count < 0) break;

                if (count == 0) {
                    int character = valueReader.read();
                    if (character < 0) break;
                    buffer[length++] = (char) character;
                } else {
                    length += count;
                }
            }
        } catch (IOException e) {
            throw new SQLException("Could not read large value.", e);
        }

        setTruncated(length > size);
        return new String(buffer, 0, Math.min(length, size));
    }

}
