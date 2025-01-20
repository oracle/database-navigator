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

package com.dbn.connection.config.ui;

import com.dbn.common.options.ui.ConfigurationEditorForm;
import com.dbn.connection.config.ConnectionDebuggerSettings;
import com.dbn.debugger.options.DebuggerTypeOption;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.util.Range;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;

public class ConnectionDebuggerSettingsForm extends ConfigurationEditorForm<ConnectionDebuggerSettings> {
    private JPanel mainPanel;
    private JCheckBox compileDependenciesCheckBox;
    private JCheckBox tcpDriverTunnelingCheckBox;
    private JTextField tcpHostTextBox;
    private JTextField tcpPortFromTextField;
    private JTextField tcpPortToTextField;
    private ComboBox<DebuggerTypeOption> debuggerTypeComboBox;

    public ConnectionDebuggerSettingsForm(ConnectionDebuggerSettings configuration) {
        super(configuration);

        initComboBox(debuggerTypeComboBox,
                DebuggerTypeOption.JDWP,
                DebuggerTypeOption.JDBC,
                DebuggerTypeOption.ASK);


        resetFormChanges();
        updateTcpFields();
        registerComponent(mainPanel);
    }

    @Override
    protected void initAccessibility() {
        setAccessibleName(tcpPortFromTextField, "TCP port lower range");
        setAccessibleName(tcpPortToTextField, "TCP port upper range");
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    protected ActionListener createActionListener() {
        return e -> {
            if (e.getSource() == tcpDriverTunnelingCheckBox) updateTcpFields();
            getConfiguration().setModified(true);
        };
    }

    @Override
    protected ItemListener createItemListener() {
        return e -> {
            if (e.getSource() == debuggerTypeComboBox) updateTcpFields();
            getConfiguration().setModified(true);
        };
    }

    private void updateTcpFields() {
        DebuggerTypeOption debuggerTypeOption = (DebuggerTypeOption) debuggerTypeComboBox.getSelectedItem();
        boolean classic = debuggerTypeOption == DebuggerTypeOption.JDBC;

        boolean tunneling = tcpDriverTunnelingCheckBox.isSelected();
        tcpDriverTunnelingCheckBox.setEnabled(!classic);
        tcpHostTextBox.setEnabled(!tunneling && !classic);
        tcpPortFromTextField.setEnabled(!tunneling && !classic);
        tcpPortToTextField.setEnabled(!tunneling && !classic);
    }

    @Override
    public void applyFormChanges() throws ConfigurationException {
        ConnectionDebuggerSettings configuration = getConfiguration();
        applyFormChanges(configuration);
    }


    @Override
    public void applyFormChanges(ConnectionDebuggerSettings configuration) throws ConfigurationException {
        configuration.setCompileDependencies(compileDependenciesCheckBox.isSelected());
        configuration.setTcpDriverTunneling(tcpDriverTunnelingCheckBox.isSelected());
        configuration.setTcpHostAddress(tcpHostTextBox.getText());
        configuration.getDebuggerType().set(getSelection(debuggerTypeComboBox));
        try {
            configuration.setTcpPortRange(new Range<>(
                    Integer.parseInt(tcpPortFromTextField.getText()),
                    Integer.parseInt(tcpPortToTextField.getText())));
        } catch (NumberFormatException e) {
            throw new ConfigurationException(txt("cfg.debugger.error.NonNumericPortRange"));
        }
    }

    @Override
    public void resetFormChanges() {
        ConnectionDebuggerSettings configuration = getConfiguration();
        compileDependenciesCheckBox.setSelected(configuration.isCompileDependencies());
        tcpDriverTunnelingCheckBox.setSelected(configuration.isTcpDriverTunneling());
        tcpHostTextBox.setText(configuration.getTcpHostAddress());
        tcpPortFromTextField.setText(String.valueOf(configuration.getTcpPortRange().getFrom()));
        tcpPortToTextField.setText(String.valueOf(configuration.getTcpPortRange().getTo()));
        setSelection(debuggerTypeComboBox, configuration.getDebuggerType().get());
        updateTcpFields();
    }
}
