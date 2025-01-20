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

import com.dbn.code.common.style.DBLCodeStyleManager;
import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.common.load.ProgressMonitor;
import com.dbn.common.util.Strings;
import com.dbn.connection.ConnectionHandler;
import com.dbn.data.export.DataExportException;
import com.dbn.data.export.DataExportFormat;
import com.dbn.data.export.DataExportInstructions;
import com.dbn.data.export.DataExportModel;
import com.dbn.data.type.GenericDataType;
import com.dbn.database.interfaces.DatabaseMetadataInterface;
import com.dbn.language.sql.SQLLanguage;
import com.intellij.openapi.project.Project;

import java.util.Date;


public class SQLDataExportProcessor extends DataExportProcessor{
    @Override
    public DataExportFormat getFormat() {
        return DataExportFormat.SQL;
    }

    @Override
    public String getFileExtension() {
        return "sql";
    }

    @Override
    public boolean supports(DataExportFeature feature) {
        return feature.isOneOf(
                DataExportFeature.EXPORT_TO_FILE,
                DataExportFeature.EXPORT_TO_CLIPBOARD,
                DataExportFeature.FILE_ENCODING);
    }

    @Override
    public String adjustFileName(String fileName) {
        if (fileName != null && !fileName.contains(".sql")) {
            fileName = fileName + ".sql";
        }
        return fileName;
    }

    @Override
    public void performExport(DataExportModel model, DataExportInstructions instructions, ConnectionHandler connection) throws DataExportException {
        // TODO SQL-Injection
        Project project = connection.getProject();
        CodeStyleCaseSettings styleCaseSettings = DBLCodeStyleManager.getInstance(project).getCodeStyleCaseSettings(SQLLanguage.INSTANCE);
        CodeStyleCaseOption kco = styleCaseSettings.getKeywordCaseOption();
        CodeStyleCaseOption oco = styleCaseSettings.getObjectCaseOption();

        StringBuilder buffer = new StringBuilder();
        for (int rowIndex=0; rowIndex < model.getRowCount(); rowIndex++) {
            buffer.append(kco.format("insert into "));
            buffer.append(oco.format(model.getTableName()));
            buffer.append(" (");

            int realColumnIndex = 0;
            for (int columnIndex=0; columnIndex < model.getColumnCount(); columnIndex++){
                GenericDataType genericDataType = model.getGenericDataType(columnIndex);
                if (genericDataType == GenericDataType.LITERAL ||
                        genericDataType == GenericDataType.NUMERIC ||
                        genericDataType == GenericDataType.DATE_TIME) {
                    if (realColumnIndex > 0) buffer.append(", ");
                    buffer.append(oco.format(model.getColumnName(columnIndex)));
                    realColumnIndex++;
                }
            }
            buffer.append(kco.format(") values ("));

            realColumnIndex = 0;
            for (int columnIndex=0; columnIndex < model.getColumnCount(); columnIndex++){
                ProgressMonitor.checkCancelled();
                GenericDataType genericDataType = model.getGenericDataType(columnIndex);
                if (genericDataType == GenericDataType.LITERAL ||
                        genericDataType == GenericDataType.NUMERIC ||
                        genericDataType == GenericDataType.DATE_TIME) {
                    if (columnIndex > 0) buffer.append(", ");
                    Object object = model.getValue(rowIndex, columnIndex);
                    String value = object == null ? null : object.toString();
                    if (value == null) {
                        buffer.append(kco.format("null"));
                    } else {
                        if (genericDataType == GenericDataType.LITERAL) {
                            buffer.append("'");
                            value = Strings.replace(value, "'", "''");
                            buffer.append(value);
                            buffer.append("'");
                        } else if (genericDataType == GenericDataType.NUMERIC) {
                            buffer.append(value);
                        } else if (genericDataType == GenericDataType.DATE_TIME) {
                            Date date = (Date) object;
                            DatabaseMetadataInterface metadata = connection.getMetadataInterface();
                            String dateString = metadata.createDateString(date);
                            buffer.append(dateString);
                        }
                    }
                    realColumnIndex++;
                }
            }

            buffer.append(");\n\n");
        }
        writeContent(instructions, buffer.toString());
    }
}
