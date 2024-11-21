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

package com.dbn.language.psql;

import com.dbn.code.psql.style.PSQLCodeStyle;
import com.dbn.code.psql.style.options.PSQLCodeStyleSettings;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.psql.dialect.PSQLLanguageDialect;
import com.dbn.language.psql.dialect.mysql.MysqlPSQLLanguageDialect;
import com.dbn.language.psql.dialect.oracle.OraclePLSQLLanguageDialect;
import com.dbn.language.psql.dialect.postgres.PostgresPSQLLanguageDialect;
import com.dbn.language.psql.dialect.sqlite.SqlitePSQLLanguageDialect;
import com.intellij.openapi.project.Project;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.Nullable;

public class PSQLLanguage extends DBLanguage<PSQLLanguageDialect> {
    public static final String ID = "DBN-PSQL";
    public static final PSQLLanguage INSTANCE = new PSQLLanguage();

    @Override
    protected PSQLLanguageDialect[] createLanguageDialects() {
        PSQLLanguageDialect oraclePLSQLLanguageDialect = new OraclePLSQLLanguageDialect();
        PSQLLanguageDialect mysqlPSQLLanguageDialect = new MysqlPSQLLanguageDialect();
        PSQLLanguageDialect postgresPSQLLanguageDialect = new PostgresPSQLLanguageDialect();
        PSQLLanguageDialect sqlitePSQLLanguageDialect = new SqlitePSQLLanguageDialect();
        return new PSQLLanguageDialect[]{
                oraclePLSQLLanguageDialect,
                mysqlPSQLLanguageDialect,
                postgresPSQLLanguageDialect,
                sqlitePSQLLanguageDialect};
    }

    @Override
    public PSQLLanguageDialect getMainLanguageDialect() {
        return getLanguageDialects()[0];
    }

    @Override
    protected IFileElementType createFileElementType() {
        return new PSQLFileElementType(this);
    }

    private PSQLLanguage() {
        super(ID, "text/plsql");
    }

    @Override
    public PSQLCodeStyleSettings codeStyleSettings(@Nullable Project project) {
        return PSQLCodeStyle.settings(project);
    }
}