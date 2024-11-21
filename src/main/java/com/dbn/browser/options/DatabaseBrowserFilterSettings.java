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

package com.dbn.browser.options;

import com.dbn.browser.options.ui.DatabaseBrowserFilterSettingsForm;
import com.dbn.common.options.CompositeProjectConfiguration;
import com.dbn.common.options.Configuration;
import com.dbn.object.filter.type.ObjectTypeFilterSettings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@EqualsAndHashCode(callSuper = false)
public class DatabaseBrowserFilterSettings
        extends CompositeProjectConfiguration<DatabaseBrowserSettings, DatabaseBrowserFilterSettingsForm> {

    private final @Getter(lazy = true) ObjectTypeFilterSettings objectTypeFilterSettings = new ObjectTypeFilterSettings(this, null);

    DatabaseBrowserFilterSettings(DatabaseBrowserSettings parent) {
        super(parent);
    }

    @NotNull
    @Override
    public DatabaseBrowserFilterSettingsForm createConfigurationEditor() {
        return new DatabaseBrowserFilterSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "filters";
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.browser.title.FilterSettings");
    }

    @Override
    public String getHelpTopic() {
        return "browserSettings";
    }

    /*********************************************************
     *                     Configuration                     *
     *********************************************************/

    @Override
    protected Configuration[] createConfigurations() {
        return new Configuration[]{getObjectTypeFilterSettings()};
    }
}
