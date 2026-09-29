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

package com.dbn.options.general.ui;

import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.options.general.WorkspaceFeature;
import com.dbn.options.general.WorkspaceSettings;
import com.intellij.openapi.options.ConfigurationException;
import org.jetbrains.annotations.NotNull;

import javax.swing.Box;
import javax.swing.JPanel;
import java.util.List;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;

public class WorkspaceSettingsForm extends ConfigurationEditorForm<WorkspaceSettings> {
    private JPanel mainPanel;
    private JPanel featuresPanel;
    private DBNCommentLabel hintLabel;

    private final List<WorkspaceFeatureItemForm> featureForms = DisposableContainers.list(this);

    public WorkspaceSettingsForm(@NotNull WorkspaceSettings settings) {
        super(settings);

        initFeaturePanel();
        resetFormChanges();
        registerComponent(mainPanel);
    }

    private void initFeaturePanel() {
        verticalBoxLayout(featuresPanel);
        for (WorkspaceFeature feature : WorkspaceFeature.values()) {
            WorkspaceFeatureItemForm form = new WorkspaceFeatureItemForm(this, feature);
            featuresPanel.add(form.getComponent());
            featureForms.add(form);
        }
        featuresPanel.add(Box.createVerticalGlue());
    }

    @Override
    public void resetFormChanges() {
        WorkspaceSettings settings = getConfiguration();
        featureForms.forEach(form -> form.setFeatureEnabled(settings.isEnabled(form.getFeature())));
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        WorkspaceSettings settings = getConfiguration();
        featureForms.forEach(form -> settings.setEnabled(form.getFeature(), form.isFeatureEnabled()));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

}
