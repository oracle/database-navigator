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

package com.dbn.language.common;

import lombok.Getter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import static com.dbn.nls.NlsResources.txt;

@Getter
public enum DBLanguageDialectIdentifier {
    ORACLE_SQL("ORACLE-SQL", txt("app.language.const.Dialect_ORACLE_SQL")),
    ORACLE_PLSQL("ORACLE-PLSQL", txt("app.language.const.Dialect_ORACLE_PLSQL")),
    MYSQL_SQL("MYSQL-SQL", txt("app.language.const.Dialect_MYSQL_SQL")),
    MYSQL_PSQL("MYSQL-PSQL", txt("app.language.const.Dialect_MYSQL_PSQL")),
    POSTGRES_SQL("POSTGRES-SQL", txt("app.language.const.Dialect_POSTGRES_SQL")),
    POSTGRES_PSQL("POSTGRES-PSQL", txt("app.language.const.Dialect_POSTGRES_PSQL")),
    SQLITE_SQL("SQLITE-SQL", txt("app.language.const.Dialect_SQLITE_SQL")),
    SQLITE_PSQL("SQLITE-PSQL", txt("app.language.const.Dialect_SQLITE_PSQL")),
    ISO92_SQL("ISO92-SQL", txt("app.language.const.Dialect_ISO92_SQL"));

    private final String value;
    private final @Nls String name;

    DBLanguageDialectIdentifier(@NonNls String value, @Nls String name) {
        this.value = value;
        this.name = name;
    }
}
