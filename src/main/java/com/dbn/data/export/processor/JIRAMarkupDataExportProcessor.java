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

import com.dbn.common.clipboard.JiraContent;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.locale.Formatter;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.export.DataExportException;
import com.dbn.data.export.DataExportFormat;
import com.dbn.data.export.DataExportInstructions;
import com.dbn.data.export.DataExportModel;
import com.dbn.data.type.GenericDataType;

import java.awt.datatransfer.Transferable;


public class JIRAMarkupDataExportProcessor extends DataExportProcessor{
    @Override
    public DataExportFormat getFormat() {
        return DataExportFormat.JIRA;
    }

    @Override
    public String getFileExtension() {
        return "txt";
    }

    @Override
    public boolean supports(DataExportFeature feature) {
        return feature.isOneOf(
                DataExportFeature.HEADER_CREATION,
                DataExportFeature.FRIENDLY_HEADER,
                DataExportFeature.EXPORT_TO_FILE,
                DataExportFeature.EXPORT_TO_CLIPBOARD,
                DataExportFeature.FILE_ENCODING);
    }

    @Override
    public String adjustFileName(String fileName) {
        if (!fileName.endsWith(".txt")) {
            fileName = fileName + ".txt";
        }
        return fileName;
    }

    @Override
    public Transferable createClipboardContent(String content) {
        return new JiraContent(content);
    }


    @Override
    public void performExport(DataExportModel model, DataExportInstructions instructions, ConnectionHandler connection) throws DataExportException {
        StringBuilder buffer = new StringBuilder();

        if (instructions.isCreateHeader()) {
            buffer.append("||");
            for (int columnIndex = 0; columnIndex < model.getColumnCount(); columnIndex++){
                String columnName = getColumnName(model, instructions, columnIndex);
                buffer.append(columnName).append("||");
            }
            buffer.append("\n");
        }

        Formatter formatter = getFormatter(connection.getProject());

        for (int rowIndex=0; rowIndex < model.getRowCount(); rowIndex++) {
            buffer.append("|");

            for (int columnIndex=0; columnIndex < model.getColumnCount(); columnIndex++){
                ProgressMonitor.checkCancelled();
                GenericDataType genericDataType = model.getGenericDataType(columnIndex);
                Object object = model.getValue(rowIndex, columnIndex);
                String value = formatValue(formatter, object);
                value = value.replaceAll("\\|", "\\|");
                value = value.replaceAll("\\*", "\\*");
                // TODO add more markup escapes

                if (Strings.isEmptyOrSpaces(value)) value = " ";

/*                boolean isNoWrap =
                        genericDataType == GenericDataType.NUMERIC ||
                        genericDataType == GenericDataType.DATE_TIME ||
                        value.length() < 100;

                boolean isAlignRight = genericDataType == GenericDataType.NUMERIC;

                if (isNoWrap) buffer.append(" nowrap");
                if (isAlignRight) buffer.append(" align=\"right\"");*/
                buffer.append(value);
                buffer.append("|");
            }
            buffer.append("\n");
        }

        writeContent(instructions, buffer.toString());
    }
}
