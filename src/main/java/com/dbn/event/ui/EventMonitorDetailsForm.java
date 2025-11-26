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

import com.dbn.common.ui.form.DBNForm;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.common.ui.tab.DBNTabbedPane;
import com.dbn.connection.ConnectionHandler;
import com.dbn.event.notification.model.DataChangeNotificationBundle;
import com.dbn.event.notification.ui.EventNotificationsForm;
import com.dbn.event.registration.model.DataChangeRegistrationBundle;
import com.dbn.event.registration.ui.EventRegistrationsForm;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static com.dbn.common.dispose.Failsafe.nd;

public class EventMonitorDetailsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel tabsPanel;
    private JPanel headerPanel;
    private final DBNTabbedPane<DBNForm> contentTabs;

    private final @Getter EventRegistrationsForm registrationsForm;
    private final @Getter EventNotificationsForm notificationsForm;

    public EventMonitorDetailsForm(@NotNull EventMonitorForm parent, ConnectionHandler connection) {
        super(parent);

        // Initialize components
        contentTabs = new DBNTabbedPane<>(this);
        tabsPanel.add(contentTabs, BorderLayout.CENTER);
        contentTabs.enableFocusInheritance();

        DataChangeNotificationBundle eventModel = new DataChangeNotificationBundle(connection);
        DataChangeRegistrationBundle registrationModel = new DataChangeRegistrationBundle(connection);

        // Initialize tables
        registrationsForm = new EventRegistrationsForm(this, registrationModel);
        contentTabs.addTab("Registrations", registrationsForm.getComponent(), registrationsForm);

        notificationsForm = new EventNotificationsForm(this, eventModel);
        contentTabs.addTab("Notifications", notificationsForm.getComponent(), notificationsForm);

        initFormHeader(connection);

        contentTabs.addTabSelectionListener(i -> {
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