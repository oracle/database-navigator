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

package com.dbn.event.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.util.TabbedPanes;
import com.dbn.connection.ConnectionHandler;
import com.dbn.event.notification.model.DataChangeNotificationBundle;
import com.dbn.event.notification.ui.EventNotificationsForm;
import com.dbn.event.registration.model.DataChangeRegistrationBundle;
import com.dbn.event.registration.ui.EventRegistrationsForm;
import com.intellij.ui.components.JBTabbedPane;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

import static com.dbn.common.dispose.Failsafe.nd;

public class EventMonitorDetailsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JBTabbedPane contentTabs;

    private final @Getter EventRegistrationsForm registrationsForm;
    private final @Getter EventNotificationsForm notificationsForm;

    public EventMonitorDetailsForm(@NotNull EventMonitorForm parent, ConnectionHandler connection) {
        super(parent);

        DataChangeNotificationBundle eventModel = new DataChangeNotificationBundle(connection);
        DataChangeRegistrationBundle registrationModel = new DataChangeRegistrationBundle(connection);

        // Initialize tables
        registrationsForm = new EventRegistrationsForm(this, registrationModel);
        contentTabs.addTab("Registrations", registrationsForm.getComponent());

        notificationsForm = new EventNotificationsForm(this, eventModel);
        contentTabs.addTab("Notifications", notificationsForm.getComponent());

        initFormHeader(connection);

        TabbedPanes.onSelectionChange(contentTabs, i -> {
            EventMonitorForm parentForm = nd(getParentComponent());
            parentForm.setTabSelectionIndex(i);

        });
    }

    private void initFormHeader(ConnectionHandler connection) {
        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent());
    }


    public void selectTab(int tabIndex) {
        contentTabs.setSelectedIndex(tabIndex);
    }


    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}