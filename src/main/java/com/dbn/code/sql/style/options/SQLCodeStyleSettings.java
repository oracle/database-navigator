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

package com.dbn.code.sql.style.options;

import com.dbn.code.common.style.options.CodeStyleCaseSettings;
import com.dbn.code.common.style.options.CodeStyleFormattingSettings;
import com.dbn.code.common.style.options.DBLCodeStyleSettings;
import com.dbn.code.sql.style.options.ui.SQLCodeStyleSettingsEditorForm;
import com.dbn.common.icon.Icons;
import com.dbn.language.sql.SQLLanguage;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class SQLCodeStyleSettings extends DBLCodeStyleSettings<DBLCodeStyleSettings, SQLCodeStyleSettingsEditorForm> {

    SQLCodeStyleSettings(DBLCodeStyleSettings parent) {
        super(parent);
    }

    @Override
    @Nls
    public String getDisplayName() {
        return "SQL";
    }

    @Override
    @Nullable
    public Icon getIcon() {
        return Icons.FILE_SQL;
    }

    @Override
    protected CodeStyleCaseSettings createCaseSettings(DBLCodeStyleSettings parent) {
        return new SQLCodeStyleCaseSettings(parent);
    }

    @Override
    protected CodeStyleFormattingSettings createAttributeSettings(DBLCodeStyleSettings parent) {
        return new SQLCodeStyleFormattingSettings(parent);
    }

    @Override
    protected String getElementName() {
        return SQLLanguage.ID;
    }

    /*********************************************************
    *                     Configuration                     *
    *********************************************************/
    @Override
    @NotNull
    public SQLCodeStyleSettingsEditorForm createConfigurationEditor() {
        return new SQLCodeStyleSettingsEditorForm(this);
    }
}
