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

import com.dbn.common.util.Files;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.mapping.FileConnectionContextManager;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.PlainSyntaxHighlighterFactory;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.dispose.Checks.isNotValid;

public class DBSyntaxHighlighterFactory extends SyntaxHighlighterFactory {
    @NotNull
    @Override
    public SyntaxHighlighter getSyntaxHighlighter(Project project, VirtualFile file) {
        if (isNotValid(project) || isNotValid(file) || !Files.isDbLanguageFile(file)) return getDefault(project, file);

        DBLanguageFileType fileType = (DBLanguageFileType) file.getFileType();
        DBLanguage language = (DBLanguage) fileType.getLanguage();

        ConnectionHandler connection = FileConnectionContextManager.getInstance(project).getConnection(file);
        DBLanguageDialect languageDialect = connection == null ?
                language.getMainLanguageDialect() :
                connection.getLanguageDialect(language);

        return languageDialect.getSyntaxHighlighter();
    }

    private static SyntaxHighlighter getDefault(Project project, VirtualFile virtualFile) {
        return PlainSyntaxHighlighterFactory.getSyntaxHighlighter(Language.ANY, project, virtualFile);
    }
}
