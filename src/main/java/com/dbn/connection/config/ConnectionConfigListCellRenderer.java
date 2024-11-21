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

package com.dbn.connection.config;

import com.dbn.common.icon.Icons;
import com.dbn.connection.ConnectivityStatus;
import com.dbn.connection.config.ui.ConnectionDatabaseSettingsForm;
import com.dbn.connection.config.ui.ConnectionSettingsForm;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JList;
import java.awt.Component;

public class ConnectionConfigListCellRenderer extends DefaultListCellRenderer{
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        ConnectionSettings connectionSettings = (ConnectionSettings) value;
        ConnectionDatabaseSettings databaseSettings = connectionSettings.getDatabaseSettings();
        JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus );
        ConnectionDatabaseSettingsForm databaseSettingsForm = databaseSettings.getSettingsEditor();
        String name = databaseSettingsForm == null ?
                databaseSettings.getName() :
                databaseSettingsForm.getConnectionName();

        ConnectivityStatus connectivityStatus = databaseSettings.getConnectivityStatus();

        ConnectionSettingsForm connectionSettingsForm = connectionSettings.getSettingsEditor();

        boolean isActive = connectionSettingsForm == null ?
                connectionSettings.isActive() :
                connectionSettingsForm.isConnectionActive();

        Icon icon = Icons.CONNECTION_DISABLED;
        boolean isNew = connectionSettings.isNew();

        if (isNew) {
            icon = connectivityStatus == ConnectivityStatus.VALID ? Icons.CONNECTION_CONNECTED_NEW : Icons.CONNECTION_NEW;
        } else if (isActive) {
            icon = connectivityStatus == ConnectivityStatus.VALID ? Icons.CONNECTION_CONNECTED :
                   connectivityStatus == ConnectivityStatus.INVALID ? Icons.CONNECTION_INVALID : Icons.CONNECTION_INACTIVE;
        }

        label.setIcon(icon);
        label.setText(name);
/*        if (!cellHasFocus && isSelected) {
            label.setForeground(actions.getForeground());
            label.setBackground(actions.hasFocus() ? actions.getBackground() : UIUtil.getFocusedFillColor());
            label.setBorder(new DottedBorder(Color.BLACK));
        }*/
        return label;
    }
}
