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

import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.locale.Formatter;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.export.DataExportException;
import com.dbn.data.export.DataExportFormat;
import com.dbn.data.export.DataExportInstructions;
import com.dbn.data.export.DataExportModel;

import static com.dbn.common.util.Spreadsheets.isSpreadsheetFormulaRisk;

public class CustomDataExportProcessor extends DataExportProcessor{
    @Override
    public DataExportFormat getFormat() {
        return DataExportFormat.CUSTOM;
    }

    @Override
    public boolean supports(DataExportFeature feature) {
        return feature.isOneOf(
                DataExportFeature.HEADER_CREATION,
                DataExportFeature.FRIENDLY_HEADER,
                DataExportFeature.EXPORT_TO_FILE,
                DataExportFeature.EXPORT_TO_CLIPBOARD,
                DataExportFeature.VALUE_QUOTING,
                DataExportFeature.FILE_ENCODING);
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public void performExport(DataExportModel model, DataExportInstructions instructions, ConnectionHandler connection) throws DataExportException {
        StringBuilder buffer = new StringBuilder();
        Formatter formatter = getFormatter(connection.getProject());

        createHeader(model, instructions, buffer);
        createContent(model, instructions, formatter, buffer);
        writeContent(instructions, buffer.toString());
    }

    private void createHeader(DataExportModel model, DataExportInstructions instructions, StringBuilder buffer) throws DataExportException {
        if (!instructions.isCreateHeader()) return;

        for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++){
            String columnName = getColumnName(model, instructions, columnIndex);
            String separator = instructions.getValueSeparator();
            if (columnIndex > 0) {
                buffer.append(separator);
            }
            appendField(buffer, columnName, instructions);
        }
        buffer.append('\n');
    }

    private void createContent(DataExportModel model, DataExportInstructions instructions, Formatter formatter, StringBuilder buffer) throws DataExportException {
        for (int r = 0; r < model.getRowCount(); r++) {
            for (int c = 0; c < model.getColumnCount(); c++) {
                ProgressMonitor.checkCancelled();
                Object object = model.getValue(r, c);
                String value = formatValue(formatter, model, object);
                String separator = instructions.getValueSeparator();
                if (c > 0) {
                    buffer.append(separator);
                }
                appendField(buffer, value, instructions);
            }
            buffer.append('\n');
        }
    }

    static void appendField(StringBuilder buffer, String value, DataExportInstructions instructions) {
        String beginQuote = instructions.getBeginQuote();
        String endQuote = instructions.getEndQuote();
        String separator = instructions.getValueSeparator();
        boolean hasBeginQuote = beginQuote != null && !beginQuote.isEmpty();
        boolean hasEndQuote = endQuote != null && !endQuote.isEmpty();
        boolean formulaRisk = isSpreadsheetFormulaRisk(value);
        boolean quote =
                instructions.isQuoteAllValues() ||
                formulaRisk ||
                (hasBeginQuote && value.contains(beginQuote)) ||
                (hasEndQuote && value.contains(endQuote)) ||
                value.contains("\r") ||
                value.contains("\n") ||
                (instructions.isQuoteValuesContainingSeparator() && value.contains(separator)) ||
                value.contains(separator);

        if (!quote) {
            buffer.append(value);
            return;
        }

        buffer.append(beginQuote);
        if (formulaRisk) {
            buffer.append('\'');
        }
        appendEscaped(buffer, value, beginQuote, endQuote);
        buffer.append(endQuote);
    }

    private static void appendEscaped(StringBuilder buffer, String value, String beginQuote, String endQuote) {
        for (int i = 0; i < value.length(); i++) {
            if (beginQuote != null && !beginQuote.isEmpty() && value.startsWith(beginQuote, i)) {
                buffer.append(beginQuote).append(beginQuote);
                i += beginQuote.length() - 1;
            } else if (endQuote != null && !endQuote.isEmpty() && !endQuote.equals(beginQuote) && value.startsWith(endQuote, i)) {
                buffer.append(endQuote).append(endQuote);
                i += endQuote.length() - 1;
            } else {
                buffer.append(value.charAt(i));
            }
        }
    }

}
