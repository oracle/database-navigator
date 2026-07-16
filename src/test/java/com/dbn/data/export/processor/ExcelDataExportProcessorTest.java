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
import com.intellij.openapi.project.Project;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class ExcelDataExportProcessorTest {
    @Test
    public void validateExportCapacityRejectsUnsafeXlsExport() throws Exception {
        ExcelDataExportProcessor processor = new ExcelDataExportProcessor();

        processor.validateExportCapacity(new TestDataExportModel(100, 10));
        assertThrows(DataExportException.class, () -> processor.validateExportCapacity(new TestDataExportModel(65536, 1)));
        assertThrows(DataExportException.class, () -> processor.validateExportCapacity(new TestDataExportModel(1, 257)));
        assertThrows(DataExportException.class, () -> processor.validateExportCapacity(new TestDataExportModel(1000, 251)));
    }

    @Test
    public void validateExportCapacityUsesXlsxLimits() throws Exception {
        ExcelXDataExportProcessor processor = new ExcelXDataExportProcessor();

        processor.validateExportCapacity(new TestDataExportModel(65536, 1));
        assertThrows(DataExportException.class, () -> processor.validateExportCapacity(new TestDataExportModel(1048576, 1)));
        assertThrows(DataExportException.class, () -> processor.validateExportCapacity(new TestDataExportModel(1, 16385)));
    }

    @Test
    public void shouldAutoSizeColumnsOnlyForSmallExports() {
        ExcelDataExportProcessor processor = new ExcelDataExportProcessor();

        assertTrue(processor.shouldAutoSizeColumns(new TestDataExportModel(100, 10)));
        assertFalse(processor.shouldAutoSizeColumns(new TestDataExportModel(1000, 251)));
    }

    private static class TestDataExportModel implements DataExportModel {
        private final int rowCount;
        private final int columnCount;
        private final List<String> warnings = new ArrayList<>();

        private TestDataExportModel(int rowCount, int columnCount) {
            this.rowCount = rowCount;
            this.columnCount = columnCount;
        }

        @Override
        public String getTableName() {
            return "TEST_TABLE";
        }

        @Override
        public int getColumnCount() {
            return columnCount;
        }

        @Override
        public int getRowCount() {
            return rowCount;
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
            return GenericDataType.LITERAL;
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
