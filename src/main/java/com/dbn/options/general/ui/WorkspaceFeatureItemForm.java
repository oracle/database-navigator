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
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Components;
import com.dbn.common.ui.util.Fonts;
import com.dbn.options.general.WorkspaceFeature;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager;

import static com.intellij.util.ui.UIUtil.getLabelDisabledForeground;
import static com.intellij.util.ui.UIUtil.getLabelForeground;

public class WorkspaceFeatureItemForm extends DBNFormBase {
    private JPanel mainPanel;
    private JCheckBox enabledCheckBox;
    private JTextPane descriptionTextPane;
    private int descriptionWidth;

    private final WorkspaceFeature feature;

    public WorkspaceFeatureItemForm(@NotNull DBNFormBase parent, @NotNull WorkspaceFeature feature) {
        super(parent);
        this.feature = feature;

        enabledCheckBox.setText(feature.getDisplayName());
        descriptionTextPane.setText(feature.getDescription());
        enabledCheckBox.setFont(Fonts.regular(1));
        descriptionTextPane.setForeground(Colors.faded(getLabelForeground()));
        descriptionTextPane.setFocusable(false);
        enabledCheckBox.addActionListener(e -> refreshState());
        refreshState();

        whenFirstShown(this::installDescriptionResizer);
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

    private void installDescriptionResizer() {
        Container container = mainPanel.getParent();
        if (!(container instanceof JPanel parent)) return;
        LayoutManager layout = parent.getLayout();
        if (!(layout instanceof BoxLayout)) return;

        Components.onComponentResized(parent, e -> resizeDescription());
        resizeDescription();
    }

    private void resizeDescription() {
        int width = descriptionTextPane.getWidth();
        if (width <= 0 || width == descriptionWidth) return;

        descriptionTextPane.setPreferredSize(null);
        descriptionTextPane.setSize(width, Integer.MAX_VALUE);
        int height = descriptionTextPane.getPreferredSize().height;
        descriptionTextPane.setPreferredSize(new Dimension(150, height));
        Dimension maximumSize = mainPanel.getMaximumSize();
        mainPanel.setMaximumSize(new Dimension(maximumSize.width, mainPanel.getPreferredSize().height));
        descriptionTextPane.revalidate();
        mainPanel.revalidate();
        descriptionWidth = width;
    }

    @Override
    protected JPanel getMainComponent() {
        return mainPanel;
    }
}
