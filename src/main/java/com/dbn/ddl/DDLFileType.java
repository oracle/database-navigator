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

package com.dbn.ddl;

import com.dbn.common.util.Files;
import com.dbn.common.util.Strings;
import com.dbn.editor.DBContentType;
import com.dbn.language.common.DBLanguageFileType;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@EqualsAndHashCode
public class DDLFileType {
    private final DBLanguageFileType languageFileType;
    private final DDLFileTypeId id;
    private final @Nls String description;
    private final DBContentType contentType;
    private final String defaultExtension;
    private Set<String> namePatterns = new LinkedHashSet<>();

    public DDLFileType(DDLFileTypeId id, @Nls String description, @NonNls String extension, DBLanguageFileType languageFileType, DBContentType contentType) {
        this.id = id;
        this.description = description;
        this.defaultExtension = extension;
        this.namePatterns.add(toFileNamePattern(extension));
        this.languageFileType = languageFileType;
        this.contentType = contentType;
    }

    public boolean setNamePatterns(Collection<String> namePatterns) {
        namePatterns = new LinkedHashSet<>(namePatterns);
        if (!namePatterns.containsAll(this.namePatterns) || !this.namePatterns.containsAll(namePatterns)) {
            this.namePatterns = (Set<String>) namePatterns;
            return true;
        }
        return false;
    }

    public String getFirstNamePattern() {
        return namePatterns.stream().findFirst().orElse(null);
    }

    public String getNamePatternsAsString() {
        return Strings.concatenate(namePatterns, ", ");
    }

    public boolean setNamePatternsAsString(String namePatterns) {
        return setNamePatterns(Strings.tokenize(namePatterns, ","));
    }

    public boolean matchesFileName(String fileName) {
        for (String namePattern : namePatterns) {
            if (matchesFileName(namePattern, fileName)) return true;
        }
        return false;
    }

    public static boolean matchesFileName(String namePattern, String fileName) {
        String regexPattern = Files.toRegexFileNamePattern(namePattern);
        return fileName.matches(regexPattern);
    }

    public static String toFileNamePattern(String extension) {
        if (Strings.isEmpty(extension)) return extension;
        if (extension.contains("*")) return extension;
        return "*." + extension;
    }
}
