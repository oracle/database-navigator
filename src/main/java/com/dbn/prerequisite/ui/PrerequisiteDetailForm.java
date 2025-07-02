/*
 * Copyright 2025 Oracle and/or its affiliates
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

package com.dbn.prerequisite.ui;

import com.dbn.common.color.Colors;
import com.dbn.common.icon.Icons;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.prerequisite.event.PrerequisiteEvent;
import com.dbn.prerequisite.event.PrerequisiteEventListener;
import com.dbn.prerequisite.event.PrerequisiteEventType;
import com.dbn.prerequisite.model.Prerequisite;
import com.dbn.prerequisite.model.PrerequisiteBundle;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.UIUtil;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;

public class PrerequisiteDetailForm extends DBNFormBase implements PrerequisiteEventListener {

    private JPanel mainPanel;
    private JPanel statusPanel;
    private JLabel titleLabel;
    private JTextPane messageTextPane;

    private final Prerequisite prerequisite;

    public PrerequisiteDetailForm(PrerequisitesForm parent, Prerequisite prerequisite) {
        super(parent);
        this.prerequisite = prerequisite;

        titleLabel.setText(prerequisite.getDefinition().getName());
        messageTextPane.setForeground(Colors.faded(UIUtil.getLabelForeground()));
    }

    private PrerequisiteBundle getBundle() {
        PrerequisitesForm parentForm = ensureParentComponent();
        return parentForm.getPrerequisiteBundle();
    }

    public void initialize() {
        statusPanel.removeAll();
        statusPanel.add(new AsyncProcessIcon("Processing..."));
        messageTextPane.setText("Verifying prerequisite");
    }

    public void complete() {
        statusPanel.removeAll();
        Exception exception = prerequisite.getStatusException();
        String message = prerequisite.getStatusMessage();


        Icon icon = exception == null ?
                Icons.COMMON_STATUS_SUCCESS :
                Icons.COMMON_STATUS_ERROR;

        statusPanel.add(new JLabel(icon));
        messageTextPane.setText(message);

    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public void eventOccurred(PrerequisiteEvent event) {
        if (event.getPrerequisite() != prerequisite) return;

        PrerequisiteEventType type = event.getType();
        switch (type) {
            case EVALUATION_STARTED: initialize(); break;
            case EVALUATION_FINISHED: complete(); break;
            case EVALUATION_FAILED: complete(); break;
            default:
        }
    }
}
