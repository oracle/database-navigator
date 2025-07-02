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

import com.dbn.common.action.DataKeys;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.util.UserInterface;
import com.dbn.prerequisite.event.PrerequisiteEvent;
import com.dbn.prerequisite.event.PrerequisiteEventListener;
import com.dbn.prerequisite.event.PrerequisiteEventType;
import com.dbn.prerequisite.model.Prerequisite;
import com.dbn.prerequisite.model.PrerequisiteBundle;
import com.intellij.util.containers.ContainerUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.util.List;
import java.util.Map;

import static com.dbn.common.ui.Layouts.verticalBoxLayout;

@SuppressWarnings("unchecked")
public class PrerequisitesForm extends DBNFormBase implements PrerequisiteEventListener {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JPanel hintPanel;
    private JPanel detailsPanel;
    private JScrollPane detailsScrollPanel;

    private final Map<String, PrerequisiteDetailForm> detailForms = ContainerUtil.createConcurrentWeakValueMap();
    private final @Getter PrerequisiteBundle prerequisiteBundle;

    public PrerequisitesForm(PrerequisitesDialog dialog) {
        super(dialog);
        prerequisiteBundle = dialog.getPrerequisites();
        prerequisiteBundle.addEventListener(this);

        initHeaderPanel();
        initDetailsPanel();
        whenShown(() -> prerequisiteBundle.evaluateAll());
    }

    private void initDetailsPanel() {
        verticalBoxLayout(detailsPanel);
        List<Prerequisite> prerequisites = prerequisiteBundle.getPrerequisites();
        for (Prerequisite prerequisite : prerequisites) {
            PrerequisiteDetailForm detailForm = new PrerequisiteDetailForm(this, prerequisite);
            detailsPanel.add(detailForm.getMainComponent());
        }
    }

    private void initHeaderPanel() {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, prerequisiteBundle.getConnection());
        headerPanel.add(headerForm.getMainComponent());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public void eventOccurred(PrerequisiteEvent event) {
        Dispatch.run(mainPanel, () -> {
            processEvent(event);
            UserInterface.repaint(mainPanel);
        });
    }

    private void processEvent(PrerequisiteEvent event) {
        PrerequisiteEventType type = event.getType();
        switch (type) {
            case EVALUATION_STARTED: onVerificationStarted(); break;
            case EVALUATION_FINISHED: onVerificationFinished(); break;
            case EVALUATION_FAILED: onVerificationFailed(); break;
        }
    }

    private void onVerificationStarted() {
    }

    private void onVerificationFinished() {
    }

    private void onVerificationFailed() {

    }

    public Object getData(@NotNull String dataId) {
        if (DataKeys.PREREQUISITES_FORM.is(dataId)) return this;
        return null;
    }
}
