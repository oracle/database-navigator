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

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.fileTypes.SyntaxHighlighterProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.dispose.Checks.isNotValid;

public class DBSyntaxHighlighterProvider implements SyntaxHighlighterProvider {

    @Override
    @Nullable
    public SyntaxHighlighter create(@NotNull FileType fileType, @Nullable Project project, @Nullable VirtualFile file) {
        if (isNotValid(project)) return null;
        if (!(fileType instanceof DBLanguageFileType)) return null;

        DBLanguageFileType dbFileType = (DBLanguageFileType) fileType;
        DBLanguage language = (DBLanguage) dbFileType.getLanguage();

        DBLanguageDialect languageDialect = DBLanguageDialect.get(language, file, project);
        if (languageDialect == null) languageDialect = language.getMainLanguageDialect();

        return languageDialect.getSyntaxHighlighter();
    }
}
