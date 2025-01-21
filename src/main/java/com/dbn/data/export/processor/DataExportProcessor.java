/*
 * Copyright 2024 Oracle and/or its affiliates
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

package com.dbn.data.export.processor;

import com.dbn.common.locale.Formatter;
import com.dbn.common.util.Commons;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.export.DataExportException;
import com.dbn.data.export.DataExportFormat;
import com.dbn.data.export.DataExportInstructions;
import com.dbn.data.export.DataExportInstructions.Scope;
import com.dbn.data.export.DataExportModel;
import com.dbn.data.value.ValueAdapter;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NonNls;

import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

@Slf4j
public abstract class DataExportProcessor {
    public abstract boolean supports(DataExportFeature feature);

    public abstract void performExport(DataExportModel model, DataExportInstructions instructions, ConnectionHandler connection) throws DataExportException;

    Formatter getFormatter(Project project) {
        return Formatter.getInstance(project).clone();
    }

    @NonNls
    public abstract String getFileExtension();

    public void export(DataExportModel model, DataExportInstructions instructions, ConnectionHandler connection)
            throws DataExportException {
        try {
            int cols = model.getColumnCount();
            int rows = model.getRowCount();
            boolean selectionScope = instructions.getScope() == Scope.SELECTION;
            if ((cols == 0 || rows == 0) && selectionScope) {
                throw new DataExportException("No content selected for export. Uncheck the Scope \"Selection\" if you want to export the entire content.");
            }
            String fileName = adjustFileName(instructions.getFileName());
            instructions.setFileName(fileName);
            performExport(model, instructions, connection);
        } catch (ProcessCanceledException e) {
            conditionallyLog(e);
        } catch (DataExportException e) {
            conditionallyLog(e);
            throw e;
        } catch (Throwable e) {
            conditionallyLog(e);
            throw new DataExportException(e.getMessage());
        }
    }

    public abstract DataExportFormat getFormat();

    void writeContent(DataExportInstructions instructions, String content) throws DataExportException {
        if (instructions.getDestination() == DataExportInstructions.Destination.CLIPBOARD) {
            writeToClipboard(content);
        } else {
            writeToFile(instructions.getFile(), content, instructions.getCharset());
        }
    }

    private void writeToFile(File file, String content, Charset charset) throws DataExportException {
        try {
            Path filePath = file.toPath();
            Files.writeString(filePath,content, charset);
        } catch (IOException e) {
            log.warn("Failed to create export file", e);
            throw new DataExportException("Failed to create export file.\nCause: " + e.getMessage());
        }
    }

    private void writeToClipboard(String content) {
        Transferable clipboardContent = createClipboardContent(content);

        CopyPasteManager copyPasteManager = CopyPasteManager.getInstance();
        copyPasteManager.setContents(clipboardContent);
    }

    public Transferable createClipboardContent(String content) {
        return new StringSelection(content);
    }

    public String adjustFileName(String fileName) {
        return fileName;
    }

    public static boolean hasTimeComponent(Date date) {
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);

        return
            calendar.get(Calendar.HOUR) != 0 ||
            calendar.get(Calendar.MINUTE) != 0 ||
            calendar.get(Calendar.SECOND) != 0 ||
            calendar.get(Calendar.MILLISECOND) != 0;
    }

    protected static String formatValue(Formatter formatter, Object value) throws DataExportException {
        if (value != null) {
            if (value instanceof Number) {
                Number number = (Number) value;
                return formatter.formatNumber(number);
            } else if (value instanceof Date) {
                Date date = (Date) value;
                return hasTimeComponent(date) ?
                        formatter.formatDateTime(date) :
                        formatter.formatDate(date);
            } else if (value instanceof ValueAdapter){
                ValueAdapter valueAdapter = (ValueAdapter) value;
                try {
                    return Commons.nvl(valueAdapter.export(), "");
                } catch (SQLException e) {
                    conditionallyLog(e);
                    throw new DataExportException("Failed to export " + valueAdapter.getGenericDataType() + " cell. Cause: "  + e.getMessage());
                }
            } else {
                return value.toString();
            }
        }
        return "";
    }

    protected String getColumnName(DataExportModel model, DataExportInstructions instructions, int columnIndex) {
        return instructions.isFriendlyHeaders() ?
                model.getColumnFriendlyName(columnIndex) :
                model.getColumnName(columnIndex);
    }
}
