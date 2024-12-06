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

package com.dbn.generator.statement;

import com.dbn.code.common.style.DBLCodeStyleManager;
import com.dbn.code.common.style.options.CodeStyleCaseOption;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.object.DBColumn;
import com.dbn.object.DBTable;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.openapi.project.Project;

import java.util.Iterator;

public class InsertStatementGenerator extends StatementGenerator {
    private final DBObjectRef<DBTable> table;

    public InsertStatementGenerator(DBTable table) {
        this.table = DBObjectRef.of(table);
    }

    public DBTable getTable() {
        return table.ensure();
    }

    @Override
    public StatementGeneratorResult generateStatement(Project project) {
        DBTable table = getTable();
        CodeStyleCaseSettings styleCaseSettings = DBLCodeStyleManager.getInstance(project).getCodeStyleCaseSettings(SQLLanguage.INSTANCE);
        CodeStyleCaseOption kco = styleCaseSettings.getKeywordCaseOption();
        CodeStyleCaseOption oco = styleCaseSettings.getObjectCaseOption();

        StatementGeneratorResult result = new StatementGeneratorResult();

        StringBuilder statement = new StringBuilder();

        statement.append(kco.format("insert into "));
        statement.append(oco.format(table.getQuotedName(false)));
        statement.append(" (\n");

        Iterator<DBColumn> columnIterator = table.getColumns().iterator();
        while (columnIterator.hasNext()) {
            DBColumn column = columnIterator.next();
            statement.append("    ");
            statement.append(oco.format(column.getQuotedName(false)));
            if (columnIterator.hasNext()) {
                statement.append(",\n");
            } else {
                statement.append(")\n");
            }
        }
        statement.append(kco.format("values (\n"));

        columnIterator = table.getColumns().iterator();
        while (columnIterator.hasNext()) {
            DBColumn column = columnIterator.next();
            statement.append("    :");
            statement.append(column.getName());
            if (columnIterator.hasNext()) {
                statement.append(",\n");
            } else {
                statement.append(")\n");
            }
        }
        statement.append(";");

        result.setStatement(statement.toString());
        return result;
    }
}
