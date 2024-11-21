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

package com.dbn.code.psql.style;

import com.dbn.code.common.style.DBLCodeStyle;
import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.code.common.style.options.CodeStyleFormattingSettings;
import com.dbn.code.psql.style.options.PSQLCodeStyleSettings;
import com.dbn.code.psql.style.options.PSQLCodeStyleSettingsWrapper;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.Nullable;

public class PSQLCodeStyle extends DBLCodeStyle {


    public static PSQLCodeStyleSettings settings(@Nullable Project project) {
        CodeStyleSettings rootSettings = rootSettings(project);
        PSQLCodeStyleSettingsWrapper wrappedSettings = rootSettings.getCustomSettings(PSQLCodeStyleSettingsWrapper.class);
        return wrappedSettings.getSettings();
    }

    public static CodeStyleCaseSettings caseSettings(Project project) {
        return settings(project).getCaseSettings();
    }

    public static CodeStyleFormattingSettings formattingSettings(Project project) {
        return settings(project).getFormattingSettings();
    }
}
