/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.options.general;

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.options.BasicProjectConfiguration;
import com.dbn.common.options.ConfigMonitor;
import com.dbn.options.ConfigId;
import com.dbn.options.ProjectSettings;
import com.dbn.options.TopLevelConfig;
import com.dbn.options.general.listener.WorkspaceSettingsListener;
import com.dbn.options.general.ui.WorkspaceSettingsForm;
import lombok.EqualsAndHashCode;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;
import java.util.Set;

import static com.dbn.common.options.ConfigActivity.CLONING;
import static com.dbn.common.options.setting.Settings.childrenOf;
import static com.dbn.common.options.setting.Settings.enumAttribute;
import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.common.options.setting.Settings.setEnumAttribute;
import static com.dbn.nls.NlsResources.txt;

@EqualsAndHashCode(callSuper = false)
public class WorkspaceSettings extends BasicProjectConfiguration<ProjectSettings, WorkspaceSettingsForm> implements TopLevelConfig {
    private final Set<WorkspaceFeature> disabledFeatures = EnumSet.noneOf(WorkspaceFeature.class);

    public WorkspaceSettings(ProjectSettings parent) {
        super(parent);
    }

    public boolean isEnabled(@NotNull WorkspaceFeature feature) {
        return !disabledFeatures.contains(feature);
    }

    public void setEnabled(@NotNull WorkspaceFeature feature, boolean enabled) {
        if (enabled) {
            disabledFeatures.remove(feature);
        } else {
            disabledFeatures.add(feature);
        }
    }

    @Override
    public WorkspaceSettingsForm createConfigurationEditor() {
        return new WorkspaceSettingsForm(this);
    }

    @Override
    public String getDisplayName() {
        return txt("cfg.workspace.title.Workspace");
    }

    @Override
    public String getConfigElementName() {
        return "workspace-settings";
    }

    @Override
    public ConfigId getConfigId() {
        return ConfigId.WORKSPACE;
    }

    @NotNull
    @Override
    public WorkspaceSettings getOriginalSettings() {
        return ProjectSettings.get(getProject()).getWorkspaceSettings();
    }

    @Override
    public void readConfiguration(Element element) {
        disabledFeatures.clear();
        for (Element child : childrenOf(element, "disabled-feature")) {
            WorkspaceFeature feature = enumAttribute(child, "id", WorkspaceFeature.class);
            if (feature != null) {
                disabledFeatures.add(feature);
            }
        }

        if (!ConfigMonitor.is(CLONING)) {
            ProjectEvents.notify(getProject(), WorkspaceSettingsListener.TOPIC,
                    listener -> listener.configurationChanged(getProject()));
        }
    }

    @Override
    public void writeConfiguration(Element element) {
        for (WorkspaceFeature feature : disabledFeatures) {
            Element child = newElement(element, "disabled-feature");
            setEnumAttribute(child, "id", feature);
        }
    }
}
