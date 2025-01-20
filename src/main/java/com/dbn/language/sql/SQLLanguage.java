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

package com.dbn.language.sql;

import com.dbn.code.sql.style.SQLCodeStyle;
import com.dbn.code.sql.style.options.SQLCodeStyleSettings;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.sql.dialect.SQLLanguageDialect;
import com.dbn.language.sql.dialect.iso92.Iso92SQLLanguageDialect;
import com.dbn.language.sql.dialect.mysql.MysqlSQLLanguageDialect;
import com.dbn.language.sql.dialect.oracle.OracleSQLLanguageDialect;
import com.dbn.language.sql.dialect.postgres.PostgresSQLLanguageDialect;
import com.dbn.language.sql.dialect.sqlite.SqliteSQLLanguageDialect;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

public class SQLLanguage extends DBLanguage<SQLLanguageDialect> {
    @NonNls
    public static final String ID = "DBN-SQL";
    public static final SQLLanguage INSTANCE = new SQLLanguage();

    @Override
    protected SQLLanguageDialect[] createLanguageDialects() {
        SQLLanguageDialect oracleSQLLanguageDialect = new OracleSQLLanguageDialect();
        SQLLanguageDialect mysqlSQLLanguageDialect = new MysqlSQLLanguageDialect();
        SQLLanguageDialect postgresSQLLanguageDialect = new PostgresSQLLanguageDialect();
        SQLLanguageDialect sqliteSQLLanguageDialect = new SqliteSQLLanguageDialect();
        SQLLanguageDialect iso92SQLLanguageDialect = new Iso92SQLLanguageDialect();
        return new SQLLanguageDialect[]{
                oracleSQLLanguageDialect,
                mysqlSQLLanguageDialect,
                postgresSQLLanguageDialect,
                sqliteSQLLanguageDialect,
                iso92SQLLanguageDialect};
    }

    @Override
    public SQLLanguageDialect getMainLanguageDialect() {
        return getLanguageDialects()[0];
    }

    @Override
    protected IFileElementType createFileElementType() {
        return new SQLFileElementType(this);
    }

    private SQLLanguage() {
        super(ID, "text/sql");
    }


    @Override
    public SQLCodeStyleSettings codeStyleSettings(@Nullable Project project) {
        return SQLCodeStyle.settings(project);
    }
}
