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

import com.dbn.common.color.Colors;
import com.dbn.common.dispose.DisposableContainers;
import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.common.ui.info.DBNCommentLabel;
import com.dbn.options.general.WorkspaceFeature;
import com.dbn.options.general.WorkspaceSettings;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.util.List;

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
        for (WorkspaceFeature feature : WorkspaceFeature.values()) {
            WorkspaceFeatureItemForm form = new WorkspaceFeatureItemForm(this, feature);
            featureForms.add(form);
        }

        int rowCount = (featureForms.size() + 1) / 2;
        featuresPanel.setLayout(new GridLayout(rowCount, 2, JBUI.scale(8), JBUI.scale(8)));
        for (int row = 0; row < rowCount; row++) {
            featuresPanel.add(new RoundedPanel(featureForms.get(row).getComponent()));

            int secondColumnIndex = row + rowCount;
            if (secondColumnIndex < featureForms.size()) {
                featuresPanel.add(new RoundedPanel(featureForms.get(secondColumnIndex).getComponent()));
            }
        }
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

    private static final class RoundedPanel extends JPanel {
        private static final int CORNER_RADIUS = 16;

        private RoundedPanel(@NotNull JComponent content) {
            super(new BorderLayout());
            setOpaque(false);
            add(content, BorderLayout.CENTER);
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);

            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                int width = getWidth() - 1;
                int height = getHeight() - 1;
                if (width <= 0 || height <= 0) return;

                int arc = JBUI.scale(CORNER_RADIUS);
                Color background = Colors.getPanelBackground();
                g.setColor(background);
                g.fillRoundRect(0, 0, width, height, arc, arc);
                g.setColor(Colors.getOutlineColor());
                g.drawRoundRect(0, 0, width, height, arc, arc);
            } finally {
                g.dispose();
            }
        }
    }

}
