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

package com.dbn.language.sql.dialect;

import com.dbn.common.latent.Latent;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguageDialectIdentifier;
import com.dbn.language.common.element.ChameleonElementType;
import com.dbn.language.sql.SQLFileElementType;
import com.dbn.language.sql.SQLLanguage;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SQLLanguageDialect extends DBLanguageDialect {
    private final Latent<ChameleonElementType> psqlChameleonElementType = Latent.basic(() -> createPsqlChameleonElementType());

    @Nullable
    private ChameleonElementType createPsqlChameleonElementType() {
        DBLanguageDialectIdentifier chameleonDialectIdentifier = getChameleonDialectIdentifier();
        if (chameleonDialectIdentifier == null) return null;

        DBLanguageDialect plsqlDialect = DBLanguageDialect.get(chameleonDialectIdentifier);
        return new ChameleonElementType(plsqlDialect, SQLLanguageDialect.this);
    }

    public SQLLanguageDialect(@NonNls @NotNull DBLanguageDialectIdentifier identifier) {
        super(identifier, SQLLanguage.INSTANCE);
    }

    @Override
    public IFileElementType createFileElementType() {
        return new SQLFileElementType(this);
    }

    @Override
    public final ChameleonElementType getChameleonTokenType(DBLanguageDialectIdentifier dialectIdentifier) {
        if (dialectIdentifier == getChameleonDialectIdentifier()) {
            return psqlChameleonElementType.get();
        }
        return super.getChameleonTokenType(dialectIdentifier);
    }

    @Nullable
    protected DBLanguageDialectIdentifier getChameleonDialectIdentifier() {
        return null;
    }

}
