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

package com.dbn.diagnostics.options.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.diagnostics.DeveloperMode;
import com.dbn.diagnostics.Diagnostics;
import com.dbn.diagnostics.Diagnostics.DatabaseLag;
import com.dbn.diagnostics.Diagnostics.DebugLogging;
import com.dbn.diagnostics.Diagnostics.Miscellaneous;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;

import static com.dbn.common.options.ui.ConfigurationEditors.validateIntegerValue;
import static com.dbn.common.text.TextContent.plain;
import static com.dbn.common.ui.util.Accessibility.setAccessibleName;
import static com.dbn.common.ui.util.Accessibility.setAccessibleUnit;

public class DiagnosticSettingsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JCheckBox developerModeCheckBox;
    private JCheckBox databaseResourcesCheckBox;
    private JCheckBox databaseAccessCheckBox;

    private JCheckBox databaseLaggingCheckBox;
    private JTextField connectivityLagTextField;
    private JTextField queryingLagTextField;
    private JTextField fetchingLagTextField;
    private JCheckBox dialogSizingCheckbox;
    private JCheckBox nativeAlertsCheckBox;
    private JCheckBox bulkActionsCheckbox;
    private JCheckBox failsafeLoggingCheckBox;
    private JCheckBox backgroundDisposerCheckBox;
    private JPanel hintPanel;
    private JBTextField developerModeTimeoutTextField;
    private JCheckBox timeoutHandlingCheckBox;

    private final DBNHintForm disclaimerForm;

    public DiagnosticSettingsForm(@Nullable Disposable parent) {
        super(parent);
        developerModeCheckBox.setSelected(Diagnostics.isDeveloperMode());
        developerModeTimeoutTextField.setText(Integer.toString(Diagnostics.getDeveloperMode().getTimeout()));

        TextContent hintText = plain("NOTE\nDeveloper Mode enables actions that can affect your system stability and data integrity. " +
                "Features like \"Slow Database Simulations\" or excessive \"Debug Logging\" are meant for diagnostic activities only " +
                "and are significantly degrading the performance of your development environment.\n\n" +
                "Please disable developer mode unless explicitly instructed to use it and properly guided throughout the process by DBN plugin developers.");
        disclaimerForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(disclaimerForm.getComponent());

        DebugLogging debugLogging = Diagnostics.getDebugLogging();
        failsafeLoggingCheckBox.setSelected(debugLogging.isFailsafeErrors());
        databaseAccessCheckBox.setSelected(debugLogging.isDatabaseAccess());
        databaseResourcesCheckBox.setSelected(debugLogging.isDatabaseResource());

        DatabaseLag databaseLag = Diagnostics.getDatabaseLag();
        databaseLaggingCheckBox.setSelected(databaseLag.isEnabled());
        connectivityLagTextField.setText(Long.toString(databaseLag.getConnectivity()));
        queryingLagTextField.setText(Long.toString(databaseLag.getQuerying()));
        fetchingLagTextField.setText(Long.toString(databaseLag.getLoading()));

        Miscellaneous miscellaneous = Diagnostics.getMiscellaneous();
        dialogSizingCheckbox.setSelected(miscellaneous.isDialogSizingReset());
        nativeAlertsCheckBox.setSelected(miscellaneous.isNativeAlertsEnabled());
        bulkActionsCheckbox.setSelected(miscellaneous.isBulkActionsEnabled());
        backgroundDisposerCheckBox.setSelected(miscellaneous.isBackgroundDisposerDisabled());
        timeoutHandlingCheckBox.setSelected(miscellaneous.isTimeoutHandlingDisabled());

        updateFields(null);

        databaseLaggingCheckBox.addActionListener(e -> updateFields(e));
        developerModeCheckBox.addActionListener(e -> updateFields(e));
    }

    @Override
    protected void initAccessibility() {
        setAccessibleUnit(connectivityLagTextField, txt("app.shared.unit.Milliseconds"));
        setAccessibleUnit(queryingLagTextField, txt("app.shared.unit.Milliseconds"));
        setAccessibleUnit(fetchingLagTextField, txt("app.shared.unit.Milliseconds"));
        setAccessibleName(developerModeTimeoutTextField, "Developer mode timeout");
    }

    private void updateFields(ActionEvent e) {
        boolean developerMode = developerModeCheckBox.isSelected();
        developerModeTimeoutTextField.setEnabled(developerMode);
        databaseAccessCheckBox.setEnabled(developerMode);
        databaseResourcesCheckBox.setEnabled(developerMode);
        databaseLaggingCheckBox.setEnabled(developerMode);
        dialogSizingCheckbox.setEnabled(developerMode);
        nativeAlertsCheckBox.setEnabled(developerMode);
        bulkActionsCheckbox.setEnabled(developerMode);
        failsafeLoggingCheckBox.setEnabled(developerMode);
        backgroundDisposerCheckBox.setEnabled(developerMode);
        timeoutHandlingCheckBox.setEnabled(developerMode);

        boolean databaseLaggingEnabled = developerMode && databaseLaggingCheckBox.isSelected();
        connectivityLagTextField.setEnabled(databaseLaggingEnabled);
        queryingLagTextField.setEnabled(databaseLaggingEnabled);
        fetchingLagTextField.setEnabled(databaseLaggingEnabled);

        disclaimerForm.setHighlighted(developerMode);
    }



    public void applyFormChanges() throws ConfigurationException {
        DeveloperMode developerMode = Diagnostics.getDeveloperMode();
        developerMode.setTimeout(getDeveloperModeTimeout());
        developerMode.setEnabled(developerModeCheckBox.isSelected());

        DebugLogging debugLogging = Diagnostics.getDebugLogging();
        debugLogging.setFailsafeErrors(failsafeLoggingCheckBox.isSelected());
        debugLogging.setDatabaseAccess(databaseAccessCheckBox.isSelected());
        debugLogging.setDatabaseResource(databaseResourcesCheckBox.isSelected());

        DatabaseLag databaseLag = Diagnostics.getDatabaseLag();
        databaseLag.setEnabled(databaseLaggingCheckBox.isSelected());
        databaseLag.setConnectivity(validateIntegerValue(connectivityLagTextField, "Connectivity Lag", true, 0, 60000, null));
        databaseLag.setQuerying(validateIntegerValue(queryingLagTextField, "Querying Lag", true, 0, 60000, null));
        databaseLag.setLoading(validateIntegerValue(fetchingLagTextField, "Fetching Lag", true, 0, 10000, null));

        Miscellaneous miscellaneous = Diagnostics.getMiscellaneous();
        miscellaneous.setDialogSizingReset(dialogSizingCheckbox.isSelected());
        miscellaneous.setNativeAlertsEnabled(nativeAlertsCheckBox.isSelected());
        miscellaneous.setBulkActionsEnabled(bulkActionsCheckbox.isSelected());
        miscellaneous.setBackgroundDisposerDisabled(backgroundDisposerCheckBox.isSelected());
        miscellaneous.setTimeoutHandlingDisabled(timeoutHandlingCheckBox.isSelected());
    }

    private int getDeveloperModeTimeout() {
        try {
            int timeout = Integer.parseInt(developerModeTimeoutTextField.getText());
            if (timeout < 1) return 1;
            if (timeout > 180) return 180;
            return timeout;
        } catch (NumberFormatException e) {
            return Diagnostics.getDeveloperMode().getTimeout();
        }
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return developerModeCheckBox;
    }
}
