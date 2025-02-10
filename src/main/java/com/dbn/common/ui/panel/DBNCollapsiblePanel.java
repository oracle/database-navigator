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

package com.dbn.common.ui.panel;

import com.dbn.common.event.ToggleListener;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Listeners;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.InputEvent;

import static com.dbn.common.ui.util.Accessibility.setAccessibleDescription;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;

public class DBNCollapsiblePanel extends DBNFormBase {

    private JLabel toggleLabel;
    private JPanel contentPanel;
    private JPanel mainPanel;
    private JLabel toggleDetailLabel;
    private DBNButtonPanel togglePanel;
    private boolean expanded;
    private final DBNCollapsibleForm contentForm;

    private final Listeners<ToggleListener> listeners = Listeners.create(this);

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public DBNCollapsiblePanel(@NotNull DBNComponent parent, DBNCollapsibleForm contentForm, boolean expanded) {
        super(parent);
        this.contentForm = contentForm;
        this.expanded = expanded;
        this.contentPanel.add(contentForm.getComponent(), BorderLayout.CENTER);

        togglePanel.setActionConsumer(e -> toggleVisibility(e));
        updateVisibility();
    }

    public void addChild(DBNCollapsiblePanel child){
        contentPanel.add(child.getMainComponent(), BorderLayout.SOUTH);
    }

    private void toggleVisibility(InputEvent e) {
        setExpanded(!expanded);
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        updateVisibility();
        listeners.notify(l -> l.toggled(expanded));
        setAccessibleName(togglePanel, getTitle() + " " + getStateName(expanded));
        setAccessibleDescription(togglePanel, expanded ? null : contentForm.getCollapsedTitleDetail());
    }

    private String getTitle() {
        return expanded ? contentForm.getExpandedTitle() : contentForm.getCollapsedTitle();
    }

    private static String getStateName(boolean expanded) {
        return expanded ? "expanded" : "collapsed";
    }

    private void updateVisibility() {
        contentPanel.setVisible(expanded);
        toggleDetailLabel.setVisible(!expanded);
        toggleDetailLabel.setText(" - " + contentForm.getCollapsedTitleDetail());
        toggleLabel.setIcon(expanded ? UIUtil.getTreeExpandedIcon() : UIUtil.getTreeCollapsedIcon());
        toggleLabel.setText(getTitle());
    }

    public void addToggleListener(ToggleListener listener) {
        listeners.add(listener);
    }
}
