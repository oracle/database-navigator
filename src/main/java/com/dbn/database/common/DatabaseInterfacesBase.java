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

package com.dbn.database.common;

import com.dbn.database.interfaces.DatabaseDebuggerInterface;
import com.dbn.database.interfaces.DatabaseInterfaces;
import com.dbn.language.common.DBLanguage;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.psql.dialect.PSQLLanguageDialect;
import com.dbn.language.sql.SQLLanguage;
import com.dbn.language.sql.dialect.SQLLanguageDialect;
import org.jetbrains.annotations.Nullable;

public abstract class DatabaseInterfacesBase implements DatabaseInterfaces {
    private final SQLLanguageDialect sqlLanguageDialect;
    private final PSQLLanguageDialect psqlLanguageDialect;

    protected DatabaseInterfacesBase(SQLLanguageDialect sqlLanguageDialect, @Nullable PSQLLanguageDialect psqlLanguageDialect) {
        this.sqlLanguageDialect = sqlLanguageDialect;
        this.psqlLanguageDialect = psqlLanguageDialect;
    }

    @Nullable
    @Override
    public DBLanguageDialect getLanguageDialect(DBLanguage<?> language) {
        if (language == SQLLanguage.INSTANCE) return sqlLanguageDialect;
        if (language == PSQLLanguage.INSTANCE) return psqlLanguageDialect;
        return null;
    }

    @Override
    public void reset() {
        getMetadataInterface().reset();
        getDataDefinitionInterface().reset();
        DatabaseDebuggerInterface debuggerInterface = getDebuggerInterface();
        if (debuggerInterface != null) debuggerInterface.reset();
    }
}
