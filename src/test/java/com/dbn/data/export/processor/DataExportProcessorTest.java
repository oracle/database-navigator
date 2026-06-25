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
import com.dbn.data.export.DataExportModel;
import com.dbn.data.type.GenericDataType;
import com.dbn.data.value.LargeObjectValue;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DataExportProcessorTest {
    @Test
    public void formatValueReadsLargeObjectWithCellLimit() throws Exception {
        TestDataExportModel model = new TestDataExportModel();
        TestLargeObjectValue value = new TestLargeObjectValue(DataExportProcessor.MAX_EXPORT_CELL_LENGTH + 1L, "partial");

        String exportValue = DataExportProcessor.formatValue(null, model, value);

        assertEquals("partial", exportValue);
        assertEquals(DataExportProcessor.MAX_EXPORT_CELL_LENGTH, value.readMaxSize);
        assertFalse(value.exportCalled);
        assertTrue(value.released);
        assertEquals(1, model.getWarnings().size());
    }

    @Test
    public void formatValueDoesNotWarnWhenLargeObjectFitsCellLimit() throws Exception {
        TestDataExportModel model = new TestDataExportModel();
        TestLargeObjectValue value = new TestLargeObjectValue(DataExportProcessor.MAX_EXPORT_CELL_LENGTH, "content");

        String exportValue = DataExportProcessor.formatValue(null, model, value);

        assertEquals("content", exportValue);
        assertEquals(DataExportProcessor.MAX_EXPORT_CELL_LENGTH, value.readMaxSize);
        assertTrue(value.released);
        assertEquals(0, model.getWarnings().size());
    }

    @Test
    public void formatValueDoesNotWarnWhenLargeObjectIsTruncatedWithoutModel() throws Exception {
        TestLargeObjectValue value = new TestLargeObjectValue(DataExportProcessor.MAX_EXPORT_CELL_LENGTH + 1L, "partial");

        String exportValue = DataExportProcessor.formatValue(null, null, value);

        assertEquals("partial", exportValue);
        assertEquals(DataExportProcessor.MAX_EXPORT_CELL_LENGTH, value.readMaxSize);
        assertTrue(value.released);
    }

    @Test
    public void formatValueReturnsEmptyStringWhenLargeObjectReadReturnsNull() throws Exception {
        TestDataExportModel model = new TestDataExportModel();
        TestLargeObjectValue value = new TestLargeObjectValue(0, null);

        String exportValue = DataExportProcessor.formatValue(null, model, value);

        assertEquals("", exportValue);
        assertEquals(DataExportProcessor.MAX_EXPORT_CELL_LENGTH, value.readMaxSize);
        assertTrue(value.released);
        assertEquals(0, model.getWarnings().size());
    }

    @Test
    public void formatValueReleasesLargeObjectWhenReadFails() {
        TestDataExportModel model = new TestDataExportModel();
        TestLargeObjectValue value = new TestLargeObjectValue(DataExportProcessor.MAX_EXPORT_CELL_LENGTH + 1L, "partial");
        value.readException = new SQLException("Read failed");

        assertThrows(DataExportException.class, () -> DataExportProcessor.formatValue(null, model, value));
        assertTrue(value.released);
    }

    private static class TestLargeObjectValue extends LargeObjectValue {
        private final long size;
        private final @Nullable String content;
        private int readMaxSize = -1;
        private boolean exportCalled;
        private boolean released;
        private SQLException readException;

        private TestLargeObjectValue(long size, @Nullable String content) {
            this.size = size;
            this.content = content;
        }

        @Override
        public GenericDataType getGenericDataType() {
            return GenericDataType.CLOB;
        }

        @Override
        public @Nullable String read() {
            exportCalled = true;
            return content;
        }

        @Override
        public @Nullable String export() {
            exportCalled = true;
            return content;
        }

        @Override
        public void write(Connection connection, PreparedStatement preparedStatement, int parameterIndex, @Nullable String value) {
        }

        @Override
        public void write(Connection connection, ResultSet resultSet, int columnIndex, @Nullable String value) {
        }

        @Override
        public String getDisplayValue() {
            return "[CLOB]";
        }

        @Override
        public @Nullable String read(int maxSize) throws SQLException {
            readMaxSize = maxSize;
            if (readException != null) throw readException;
            setTruncated(maxSize > 0 && size > maxSize);
            return content;
        }

        @Override
        public long size() throws SQLException {
            return size;
        }

        @Override
        public void release() {
            released = true;
        }
    }

    private static class TestDataExportModel implements DataExportModel {
        private final List<String> warnings = new ArrayList<>();

        @Override
        public String getTableName() {
            return "TEST_TABLE";
        }

        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public int getRowCount() {
            return 1;
        }

        @Override
        public Object getValue(int rowIndex, int columnIndex) {
            return null;
        }

        @Override
        public String getColumnName(int columnIndex) {
            return "TEST_COLUMN";
        }

        @Override
        public String getColumnFriendlyName(int columnIndex) {
            return getColumnName(columnIndex);
        }

        @Override
        public GenericDataType getGenericDataType(int columnIndex) {
            return GenericDataType.CLOB;
        }

        @Override
        public Project getProject() {
            return null;
        }

        @Override
        public List<String> getWarnings() {
            return warnings;
        }

        @Override
        public void addWarning(String warning) {
            if (!warnings.contains(warning)) warnings.add(warning);
        }
    }
}
