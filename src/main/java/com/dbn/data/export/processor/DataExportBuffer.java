/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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

package com.dbn.data.export.processor;

import com.dbn.data.export.DataExportException;

import static com.dbn.nls.NlsResources.txt;

class DataExportBuffer {
    static final int MAX_EXPORT_CONTENT_LENGTH = 50 * 1024 * 1024;

    private final int maxLength;
    private final StringBuilder buffer = new StringBuilder();

    DataExportBuffer() {
        this(MAX_EXPORT_CONTENT_LENGTH);
    }

    DataExportBuffer(int maxLength) {
        this.maxLength = maxLength;
    }

    DataExportBuffer append(String value) throws DataExportException {
        value = String.valueOf(value);
        checkLength(value.length());
        buffer.append(value);
        return this;
    }

    DataExportBuffer append(char value) throws DataExportException {
        checkLength(1);
        buffer.append(value);
        return this;
    }

    DataExportBuffer append(int value) throws DataExportException {
        return append(String.valueOf(value));
    }

    private void checkLength(int length) throws DataExportException {
        if (length > maxLength - buffer.length()) {
            throw new DataExportException(txt("msg.dataExport.error.ExportContentTooLarge", maxLength));
        }
    }

    @Override
    public String toString() {
        return buffer.toString();
    }
}
