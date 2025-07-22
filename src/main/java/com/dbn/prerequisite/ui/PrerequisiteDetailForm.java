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
import com.dbn.common.ui.text.HiddenCaret;
import com.dbn.common.ui.util.Fonts;
import com.dbn.prerequisite.definition.PrerequisiteDefinition;
import com.dbn.prerequisite.event.PrerequisiteEvent;
import com.dbn.prerequisite.event.PrerequisiteEventListener;
import com.dbn.prerequisite.event.PrerequisiteEventType;
import com.dbn.prerequisite.model.Prerequisite;
import com.dbn.prerequisite.model.PrerequisiteGroup;
import com.dbn.prerequisite.model.PrerequisiteMandate;
import com.dbn.prerequisite.model.PrerequisiteStatus;
import com.dbn.prerequisite.model.PrerequisiteType;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.Color;
import java.awt.Font;

public class PrerequisiteDetailForm extends DBNFormBase implements PrerequisiteEventListener {

    private JPanel mainPanel;
    private JPanel statusPanel;
    private JLabel titleLabel;
    private JTextPane descriptionTextArea;
    private JLabel statusLabel;
    private JPanel actionsPanel;
    private JTextPane reasonTextArea;

    private final Prerequisite prerequisite;

    public PrerequisiteDetailForm(PrerequisitesForm parent, Prerequisite prerequisite) {
        super(parent);
        this.prerequisite = prerequisite;

        parent.getPrerequisiteGroup().addEventListener(this);

        Color greyContent = Colors.faded(UIUtil.getLabelForeground());
        Font largerFont = Fonts.regular(1);

        PrerequisiteDefinition definition = prerequisite.getDefinition();
        titleLabel.setText(definition.getName());
        titleLabel.setFont(largerFont);
        statusLabel.setForeground(greyContent);
        descriptionTextArea.setFont(JBUI.Fonts.label());
        //descriptionTextArea.setForeground(greyContent);
        descriptionTextArea.setText(definition.getDescription());
        descriptionTextArea.setCaret(new HiddenCaret());

        PrerequisiteType type = prerequisite.getType();
        PrerequisiteMandate mandate = getPrerequisiteGroup().getMandate(type);
        reasonTextArea.setText(mandate.getReason());
        reasonTextArea.setFont(JBUI.Fonts.label());
        reasonTextArea.setForeground(greyContent);
        reasonTextArea.setCaret(new HiddenCaret());


        updatePrerequisiteStatus();
    }

    private PrerequisiteGroup getPrerequisiteGroup() {
        PrerequisitesForm parentForm = ensureParentComponent();
        return parentForm.getPrerequisiteGroup();
    }

    public void initialize() {
        statusPanel.removeAll();
        statusPanel.add(new AsyncProcessIcon("Verifying prerequisite..."));
        statusLabel.setText("Verifying...");
        //messageTextPane.setText("Verifying prerequisite");
    }

    public void complete() {
        updatePrerequisiteStatus();
    }

    private void updatePrerequisiteStatus() {
        statusPanel.removeAll();
        PrerequisiteStatus status = prerequisite.getStatus();
        Exception exception = prerequisite.getStatusException();
        String message = prerequisite.getStatusMessage();

        statusLabel.setToolTipText(message);
        if (exception != null) {
            statusLabel.setText("Unknown");
        } else if (status == PrerequisiteStatus.AVAILABLE) {
            statusLabel.setText("OK");
        } else if (status == PrerequisiteStatus.UNAVAILABLE) {
            statusLabel.setText("Not OK");
        }

        Icon icon = exception == null && status == PrerequisiteStatus.AVAILABLE ?
                Icons.COMMON_STATUS_SUCCESS :
                Icons.COMMON_STATUS_ERROR;

        statusPanel.add(new JLabel(icon));
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
