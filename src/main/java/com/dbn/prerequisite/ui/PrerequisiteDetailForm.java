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
import com.dbn.connection.ConnectionHandler;
import com.dbn.prerequisite.definition.PrerequisiteDefinition;
import com.dbn.prerequisite.event.PrerequisiteEvent;
import com.dbn.prerequisite.event.PrerequisiteEventListener;
import com.dbn.prerequisite.event.PrerequisiteEventType;
import com.dbn.prerequisite.model.Prerequisite;
import com.dbn.prerequisite.model.PrerequisiteBundle;
import com.dbn.prerequisite.model.PrerequisiteStatus;
import com.dbn.prerequisite.resolution.PrerequisiteAdvice;
import com.dbn.prerequisite.resolution.PrerequisiteAdvisor;
import com.intellij.util.ui.AsyncProcessIcon;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Color;

public class PrerequisiteDetailForm extends DBNFormBase implements PrerequisiteEventListener {

    private JPanel mainPanel;
    private JPanel statusPanel;
    private JLabel titleLabel;
    private JTextArea descriptionTextArea;
    private JTextArea adviceCodeTextArea;
    private JLabel adviceTextLabel;

    private final Prerequisite prerequisite;

    public PrerequisiteDetailForm(PrerequisitesForm parent, Prerequisite prerequisite) {
        super(parent);
        this.prerequisite = prerequisite;

        parent.getPrerequisiteBundle().addEventListener(this);

        PrerequisiteDefinition definition = prerequisite.getDefinition();
        titleLabel.setText(definition.getName());
        descriptionTextArea.setFont(JBUI.Fonts.label());
        descriptionTextArea.setForeground(Colors.faded(UIUtil.getLabelForeground()));
        descriptionTextArea.setText(definition.getDescription());

        ConnectionHandler connection = parent.getPrerequisiteBundle().getConnection();
        PrerequisiteAdvisor advisor = prerequisite.getDefinition().getAdvisor();
        PrerequisiteAdvice advice = advisor.advise(connection);

        adviceTextLabel.setText(advice.getDescription());
        adviceTextLabel.setVisible(false);
        adviceCodeTextArea.setText(advice.getCode());

        Color background = Colors.lafBrighter(Colors.getEditorBackground(), 5);
        adviceCodeTextArea.setBackground(background);
    }

    private PrerequisiteBundle getBundle() {
        PrerequisitesForm parentForm = ensureParentComponent();
        return parentForm.getPrerequisiteBundle();
    }

    public void initialize() {
        statusPanel.removeAll();
        statusPanel.add(new AsyncProcessIcon("Processing..."));
        //messageTextPane.setText("Verifying prerequisite");
    }

    public void complete() {
        statusPanel.removeAll();
        Exception exception = prerequisite.getStatusException();
        String message = prerequisite.getStatusMessage();


        Icon icon = exception == null && prerequisite.getStatus() == PrerequisiteStatus.SATISFIED?
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
