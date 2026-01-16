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
import com.dbn.common.text.TextContent;
import com.dbn.common.ui.component.DBNComponent;
import com.dbn.common.ui.form.DBNCollapsibleForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.info.DBNInfoLabel;
import com.dbn.common.ui.util.Listeners;
import com.dbn.common.util.Strings;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.InputEvent;

import static com.dbn.common.ui.util.Accessibility.setAccessibleDescription;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.ClientProperty.NON_DISABLEABLE;

public class DBNCollapsiblePanel extends DBNFormBase {

    private JLabel toggleLabel;
    private JPanel contentPanel;
    private JPanel mainPanel;
    private JLabel toggleDetailLabel;
    private DBNButtonPanel togglePanel;
    private JPanel contentRootPanel;
    private JPanel infoPanel;

    private TextContent infoContent;
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
        this.contentPanel.add(contentForm.getComponent());
        this.toggleDetailLabel.setForeground(UIUtil.getLabelDisabledForeground());
        NON_DISABLEABLE.set(toggleLabel, true);
        NON_DISABLEABLE.set(toggleDetailLabel, true);
        setInfoContent(null);

        togglePanel.setActionConsumer(e -> toggleVisibility(e));
        updateComponents();
    }

    protected void initAccessibility() {
        setAccessibleName(togglePanel, contentForm.getFormTitle() + " " + getStateName(expanded));
        setAccessibleDescription(togglePanel, expanded ? null : contentForm.getFormTitleDetail());
    }

    public void addChild(DBNCollapsiblePanel child){
        contentPanel.add(child.getComponent(), BorderLayout.SOUTH);
    }

    private void toggleVisibility(InputEvent e) {
        setExpanded(!expanded);
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
        updateComponents();
        initAccessibility();
        listeners.notify(l -> l.toggled(expanded));
    }

    public void setInfoContent(@Nullable TextContent infoContent) {
        this.infoContent = infoContent;
        infoPanel.setVisible(infoContent != null && expanded);
        if (infoContent != null) {
            DBNInfoLabel infoLabel = new DBNInfoLabel();
            infoLabel.setContent(infoContent);
            infoPanel.add(infoLabel);
        }
    }

    private static String getStateName(boolean expanded) {
        return expanded ? "expanded" : "collapsed";
    }

    public void updateComponents() {
        toggleLabel.setIcon(expanded ? UIUtil.getTreeExpandedIcon() : UIUtil.getTreeCollapsedIcon());
        toggleLabel.setText(contentForm.getFormTitle());

        infoPanel.setVisible(expanded && infoContent != null);
        contentRootPanel.setVisible(expanded);
        toggleDetailLabel.setVisible(!expanded);

        String detail = contentForm.getFormTitleDetail();
        toggleDetailLabel.setText(Strings.isEmpty(detail) ? "" : "(" + detail + ")");
    }

    public void addToggleListener(ToggleListener listener) {
        listeners.add(listener);
    }
}
