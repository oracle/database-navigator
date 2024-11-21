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

import com.dbn.browser.options.ui.DatabaseBrowserGeneralSettingsForm;
import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.options.setting.BooleanSetting;
import com.dbn.common.options.setting.IntegerSetting;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.options.setting.Settings.getEnum;
import static com.dbn.common.options.setting.Settings.setEnum;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false)
public class DatabaseBrowserGeneralSettings
        extends BasicProjectConfiguration<DatabaseBrowserSettings, DatabaseBrowserGeneralSettingsForm> {

    private BrowserDisplayMode displayMode = BrowserDisplayMode.TABBED;
    private final IntegerSetting navigationHistorySize = new IntegerSetting("navigation-history-size", 100);
    private final BooleanSetting showObjectDetails = new BooleanSetting("show-object-details", false);
    private final BooleanSetting enableStickyPaths = new BooleanSetting("enable-sticky-paths", true);

    public boolean isShowObjectDetails() {
        return showObjectDetails.getValue();
    }

    public boolean isEnableStickyPaths() {
        return enableStickyPaths.getValue();
    }

    DatabaseBrowserGeneralSettings(DatabaseBrowserSettings parent) {
        super(parent);
    }

    @NotNull
    @Override
    public DatabaseBrowserGeneralSettingsForm createConfigurationEditor() {
        return new DatabaseBrowserGeneralSettingsForm(this);
    }

    @Override
    public String getConfigElementName() {
        return "general";
    }

    @Override
    public void readConfiguration(Element element) {
        displayMode = getEnum(element, "display-mode", BrowserDisplayMode.TABBED);
        navigationHistorySize.readConfiguration(element);
        showObjectDetails.readConfiguration(element);
        enableStickyPaths.readConfiguration(element);
    }

    @Override
    public void writeConfiguration(Element element) {
        setEnum(element, "display-mode", displayMode);
        navigationHistorySize.writeConfiguration(element);
        showObjectDetails.writeConfiguration(element);
        enableStickyPaths.writeConfiguration(element);
    }

}
