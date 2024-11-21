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

package com.dbn.language.psql.dialect;

import com.dbn.language.common.ChameleonTokenType;
import com.dbn.language.common.DBLanguageDialect;
import com.dbn.language.common.DBLanguageDialectIdentifier;
import com.dbn.language.psql.PSQLFileElementType;
import com.dbn.language.psql.PSQLLanguage;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public abstract class PSQLLanguageDialect extends DBLanguageDialect {
    public PSQLLanguageDialect(@NonNls @NotNull DBLanguageDialectIdentifier identifier) {
        super(identifier, PSQLLanguage.INSTANCE);
    }

    @Override
    protected Set<ChameleonTokenType> createChameleonTokenTypes() {return null;}

    @Override
    public IFileElementType createFileElementType() {
        return new PSQLFileElementType(this);
    }
}