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

import com.dbn.language.common.DBLanguageParser;
import com.dbn.language.common.DBLanguageParserDefinition;
import com.dbn.language.common.TokenTypeBundle;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;


public class PSQLParserDefinition extends DBLanguageParserDefinition {

    public PSQLParserDefinition() {
        super(() -> getDefaultParseDefinition().getParser());
    }

    public PSQLParserDefinition(PSQLParser parser) {
        super(parser);
    }

    @Override
    @NotNull
    public Lexer createLexer(Project project) {
        return getDefaultParseDefinition().createLexer(project);
    }

    @NotNull
    private static DBLanguageParserDefinition getDefaultParseDefinition() {
        return PSQLLanguage.INSTANCE.getMainLanguageDialect().getParserDefinition();
    }

    @Override
    @NotNull
    public DBLanguageParser createParser(Project project) {
        return getParser();
    }

    public TokenTypeBundle getTokenTypes() {
        return getParser().getTokenTypes();
    }

    @NotNull
    @Override
    protected PsiFile createPsiFile(FileViewProvider viewProvider) {
        return new PSQLFile(viewProvider);
    }
}