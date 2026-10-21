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

package com.dbn.data.export;

import com.dbn.common.resource.ResourceLimit;
import com.dbn.common.resource.ResourceLimitPolicy;
import com.dbn.common.resource.ResourceLimits;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import static com.dbn.nls.NlsResources.txt;

/**
 * Resource bounds for export capacity, cell values, and buffered output.
 * String-length limits are measured in Java characters.
 */
@UtilityClass
public class DataExportResourceLimits implements ResourceLimitPolicy {
    public static final int MAX_XLS_DATA_ROWS = 65535;
    public static final int MAX_XLS_COLUMNS = 256;
    public static final int MAX_XLS_EXPORT_CELLS = 250000;
    public static final int MAX_XLSX_DATA_ROWS = 1048575;
    public static final int MAX_XLSX_COLUMNS = 16384;
    public static final int MAX_AUTO_SIZE_CELLS = 250000;
    public static final int MAX_EXCEL_CELL_LENGTH = 32767;
    public static final int MAX_EXPORT_CELL_LENGTH = 1024 * 1024;
    public static final int MAX_EXPORT_CONTENT_LENGTH = 50 * 1024 * 1024;

    private static final ResourceLimits XLS_LIMITS = ResourceLimits.builder()
            .add("export-data-rows", MAX_XLS_DATA_ROWS)
            .add("export-columns", MAX_XLS_COLUMNS)
            .add("in-memory-export-cells", MAX_XLS_EXPORT_CELLS)
            .build();

    private static final ResourceLimits XLSX_LIMITS = ResourceLimits.builder()
            .add("export-data-rows", MAX_XLSX_DATA_ROWS)
            .add("export-columns", MAX_XLSX_COLUMNS)
            .add("in-memory-export-cells", Long.MAX_VALUE)
            .build();

    private static final ResourceLimits LIMITS = ResourceLimits.builder()
            .add("auto-sized-export-cells", MAX_AUTO_SIZE_CELLS)
            .add("excel-cell-characters", MAX_EXCEL_CELL_LENGTH)
            .add("buffered-export-characters", MAX_EXPORT_CONTENT_LENGTH)
            .build();

    public static void verifyExcelExportCapacity(@NotNull DataExportModel model, @NotNull DataExportFormat format) throws DataExportException {
        ResourceLimits limits = switch (format) {
            case EXCEL -> XLS_LIMITS;
            case EXCELX -> XLSX_LIMITS;
            default -> throw new IllegalArgumentException("Unsupported Excel export format: " + format);
        };
        String extension = format == DataExportFormat.EXCEL ? "xls" : "xlsx";
        int rowCount = model.getRowCount();
        int columnCount = model.getColumnCount();

        if (!limits.allows("export-data-rows", rowCount)) {
            throw new DataExportException(txt("msg.dataExport.error.ExcelRowLimitExceeded", limits.get("export-data-rows").getMaximum(), extension));
        }
        if (!limits.allows("export-columns", columnCount)) {
            throw new DataExportException(txt("msg.dataExport.error.ExcelColumnLimitExceeded", limits.get("export-columns").getMaximum(), extension));
        }
        ResourceLimit cells = limits.get("in-memory-export-cells");
        if (!cells.allowsProduct(rowCount, columnCount)) {
            throw new DataExportException(txt("msg.dataExport.error.ExcelCellLimitExceeded", cells.getMaximum(), extension));
        }
    }

    public static boolean isExcelAutoSizingAllowed(@NotNull DataExportModel model) {
        return LIMITS.get("auto-sized-export-cells").allowsProduct(model.getRowCount(), model.getColumnCount());
    }

    public static boolean isExcelCellLengthAllowed(long length) {
        return LIMITS.allows("excel-cell-characters", length);
    }

    @NotNull
    public static ResourceLimit getDataExportContentLimit(int maximumLength) {
        if (maximumLength == MAX_EXPORT_CONTENT_LENGTH) return LIMITS.get("buffered-export-characters");
        return ResourceLimits.builder()
                .add("buffered-export-characters", maximumLength)
                .build()
                .get("buffered-export-characters");
    }
}
