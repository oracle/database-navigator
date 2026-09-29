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

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.common.ui.util.Fonts;
import com.dbn.options.general.WorkspaceFeature;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import static com.dbn.common.color.Colors.getLabelDisabledForeground;
import static com.dbn.common.color.Colors.getLabelForeground;


public class WorkspaceFeatureItemForm extends DBNFormBase {
    private JPanel mainPanel;
    private JCheckBox enabledCheckBox;
    private DBNCommentLabel descriptionLabel;

    private final WorkspaceFeature feature;

    public WorkspaceFeatureItemForm(@NotNull DBNFormBase parent, @NotNull WorkspaceFeature feature) {
        super(parent);
        this.feature = feature;

        enabledCheckBox.setText(feature.getDisplayName());
        descriptionLabel.setText(feature.getDescription());
        enabledCheckBox.setFont(Fonts.regular(1));
        enabledCheckBox.addActionListener(e -> refreshState());
        refreshState();
    }

    @NotNull
    public WorkspaceFeature getFeature() {
        return feature;
    }

    public boolean isFeatureEnabled() {
        return enabledCheckBox.isSelected();
    }

    public void setFeatureEnabled(boolean enabled) {
        enabledCheckBox.setSelected(enabled);
        refreshState();
    }

    private void refreshState() {
        enabledCheckBox.setForeground(enabledCheckBox.isSelected() ?
                getLabelForeground() :
                getLabelDisabledForeground());
    }

    @Override
    protected JPanel getMainComponent() {
        return mainPanel;
    }
}
