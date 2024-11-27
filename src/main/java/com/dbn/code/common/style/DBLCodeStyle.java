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

package com.dbn.code.common.style;

import com.dbn.code.common.style.options.DBLCodeStyleSettings;
import com.dbn.code.psql.style.PSQLCodeStyle;
import com.dbn.code.sql.style.SQLCodeStyle;
import com.dbn.language.psql.PSQLLanguage;
import com.dbn.language.sql.SQLLanguage;
import com.intellij.application.options.CodeStyle;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import org.jetbrains.annotations.Nullable;

public class DBLCodeStyle {
    protected static CodeStyleSettings rootSettings(@Nullable Project project) {
        if (true) {
            return CodeStyle.getProjectOrDefaultSettings(project);
        }

        CodeStyleSettings codeStyleSettings;
        if (CodeStyleSettingsManager.getInstance().USE_PER_PROJECT_SETTINGS) {
            codeStyleSettings = CodeStyleSettingsManager.getSettings(project);
        } else {
            codeStyleSettings = CodeStyleSettingsManager.getInstance().getCurrentSettings();
        }
        return codeStyleSettings;
    }

    protected static DBLCodeStyleSettings settings(Project project, Language language) {
        if (language == SQLLanguage.INSTANCE) return SQLCodeStyle.settings(project);
        if (language == PSQLLanguage.INSTANCE) return PSQLCodeStyle.settings(project);
        throw new IllegalArgumentException("Language " + language.getID() + " mot supported");
    }
}
