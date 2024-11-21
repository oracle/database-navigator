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

package com.dbn.ddl.options;

import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.options.setting.BooleanSetting;
import com.dbn.ddl.options.ui.DDLFileGeneralSettingsForm;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class DDLFileGeneralSettings extends BasicProjectConfiguration<DDLFileSettings, DDLFileGeneralSettingsForm> {
    private final BooleanSetting ddlFilesLookupEnabled = new BooleanSetting("lookup-ddl-files", true);
    private final BooleanSetting ddlFilesCreationEnabled = new BooleanSetting("create-ddl-files", false);
    private final BooleanSetting ddlFilesSynchronizationEnabled = new BooleanSetting("synchronize-ddl-files", true);
    private final BooleanSetting useQualifiedObjectNames = new BooleanSetting("use-qualified-names", false);
    private final BooleanSetting makeScriptsRerunnable = new BooleanSetting("make-scripts-rerunnable", true);

    DDLFileGeneralSettings(DDLFileSettings parent) {
        super(parent);
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.ddlFiles.title.DdlFileGeneralSettings");
    }

    public boolean isDdlFilesLookupEnabled() {
        return ddlFilesLookupEnabled.value();
    }

    public boolean isDdlFilesCreationEnabled() {
        return ddlFilesCreationEnabled.value();
    }

    public boolean isDdlFilesSynchronizationEnabled() {
        return ddlFilesSynchronizationEnabled.value();
    }

    public boolean isUseQualifiedObjectNames() {
        return useQualifiedObjectNames.value();
    }

    public boolean isMakeScriptsRerunnable() {
        return makeScriptsRerunnable.value();
    }

    /*********************************************************
     *                     Configuration                     *
     *********************************************************/
    @Override
    @NotNull
    public DDLFileGeneralSettingsForm createConfigurationEditor() {
        return new DDLFileGeneralSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "general";
    }

    @Override
    public void readConfiguration(Element element) {
        ddlFilesLookupEnabled.readConfiguration(element);
        ddlFilesCreationEnabled.readConfiguration(element);
        ddlFilesSynchronizationEnabled.readConfiguration(element);
        useQualifiedObjectNames.readConfiguration(element);
        makeScriptsRerunnable.readConfiguration(element);
    }

    @Override
    public void writeConfiguration(Element element) {
        ddlFilesLookupEnabled.writeConfiguration(element);
        ddlFilesCreationEnabled.writeConfiguration(element);
        ddlFilesSynchronizationEnabled.writeConfiguration(element);
        useQualifiedObjectNames.writeConfiguration(element);
        makeScriptsRerunnable.writeConfiguration(element);
    }
}
