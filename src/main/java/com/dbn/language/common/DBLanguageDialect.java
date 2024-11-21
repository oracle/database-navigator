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

import com.dbn.common.action.UserDataKeys;
import com.dbn.common.dispose.Failsafe;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.dbn.language.common.element.ChameleonElementType;
import com.dbn.language.common.element.TokenPairTemplate;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.sql.SQLLanguage;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.tree.IFileElementType;
import lombok.Getter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.dbn.common.dispose.Checks.isNotValid;

@Getter
public abstract class DBLanguageDialect extends Language implements DBFileElementTypeProvider {

    private static final Map<DBLanguageDialectIdentifier, DBLanguageDialect> REGISTRY = new EnumMap<>(DBLanguageDialectIdentifier.class);
    private static final TokenPairTemplate[] TOKEN_PAIR_TEMPLATES = new TokenPairTemplate[] {TokenPairTemplate.PARENTHESES};

    private final DBLanguageDialectIdentifier identifier;

    private final @Getter(lazy = true) DBLanguageSyntaxHighlighter syntaxHighlighter = createSyntaxHighlighter();
    private final @Getter(lazy = true) DBLanguageParserDefinition parserDefinition = createParserDefinition();
    private final @Getter(lazy = true) IFileElementType fileElementType = createFileElementType();

    private Set<ChameleonTokenType> chameleonTokens;

    public DBLanguageDialect(@NonNls @NotNull DBLanguageDialectIdentifier identifier, @NotNull DBLanguage baseLanguage) {
        super(baseLanguage, identifier.getValue());
        this.identifier = identifier;
        REGISTRY.put(identifier, this);
    }

    protected abstract Set<ChameleonTokenType> createChameleonTokenTypes();
    protected abstract DBLanguageSyntaxHighlighter createSyntaxHighlighter() ;
    protected abstract DBLanguageParserDefinition createParserDefinition();
    protected abstract IFileElementType createFileElementType();

    public ChameleonElementType getChameleonTokenType(DBLanguageDialectIdentifier dialectIdentifier) {
        throw new IllegalArgumentException("Language " + getID() + " does not support chameleons of type " + dialectIdentifier.getValue() );
    }

    public boolean isReservedWord(String identifier) {
        return getParserTokenTypes().isReservedWord(identifier);
    }

    @Override
    @NotNull
    public DBLanguage getBaseLanguage() {
        return Failsafe.nn((DBLanguage) super.getBaseLanguage());
    }

    public SharedTokenTypeBundle getSharedTokenTypes() {
        return getBaseLanguage().getSharedTokenTypes();
    }

    public TokenTypeBundle getParserTokenTypes() {
        return getParserDefinition().getParser().getTokenTypes();
    }

    public TokenTypeBundle getHighlighterTokenTypes() {
        return getSyntaxHighlighter().getTokenTypes();
    }

    public TokenType getInjectedLanguageToken(DBLanguageDialectIdentifier dialectIdentifier) {
        if (chameleonTokens == null) {
            chameleonTokens = createChameleonTokenTypes();
            if (chameleonTokens == null) chameleonTokens = new HashSet<>();
        }
        for (ChameleonTokenType chameleonToken : chameleonTokens) {
            if (chameleonToken.getInjectedLanguage().identifier == dialectIdentifier) {
                return chameleonToken;
            }
        }
        return null;
    }

    public TokenPairTemplate[] getTokenPairTemplates() {
        return TOKEN_PAIR_TEMPLATES;
    }


    public static DBLanguageDialect get(DBLanguageDialectIdentifier identifier) {
         // make sure all dialects are loaded before doing this lookup
         SQLLanguage.INSTANCE.getLanguageDialects();
         PSQLLanguage.INSTANCE.getLanguageDialects();
         return REGISTRY.get(identifier);
    }

    @Nullable
    public static DBLanguageDialect get(DBLanguage language, @Nullable ConnectionHandler connection) {
        if (connection == null) return null;
        return connection.getLanguageDialect(language);

    }

    @Nullable
    public static DBLanguageDialect get(@NotNull DBLanguage language, @Nullable VirtualFile file, @Nullable Project project) {
        if (isNotValid(project)) return null;
        if (isNotValid(file)) return null;

        DBLanguageDialect languageDialect = file.getUserData(UserDataKeys.LANGUAGE_DIALECT);
        if (languageDialect != null) return languageDialect;

        FileConnectionContextManager contextManager = FileConnectionContextManager.getInstance(project);
        ConnectionHandler connection = contextManager.getConnection(file);
        if (connection == null) return null;

        return connection.getLanguageDialect(language);
    }
}
