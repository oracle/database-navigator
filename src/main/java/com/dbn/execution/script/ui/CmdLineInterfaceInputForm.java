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

package com.dbn.execution.script.ui;

import com.dbn.common.text.TextContent;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHintForm;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.common.util.Strings;
import com.dbn.connection.DatabaseType;
import com.dbn.execution.script.CmdLineInterface;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import org.jdesktop.swingx.util.OS;
import org.jetbrains.annotations.NotNull;

import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.io.File;
import java.util.Set;

import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.common.util.Naming.nextNumberedIdentifier;
import static com.dbn.common.util.Strings.trim;
import static com.dbn.nls.NlsResources.txt;
import static org.apache.commons.lang3.ObjectUtils.isNotEmpty;

public class CmdLineInterfaceInputForm extends DBNFormBase {
    private JPanel mainPanel;
    private JTextField nameTextField;
    private JPanel hintPanel;
    private TextFieldWithBrowseButton executableTextField;
    private DBNComboBox<DatabaseType> databaseTypeComboBox;

    private final Set<String> usedNames;
    private final CmdLineInterface cmdLineInterface;

    public CmdLineInterfaceInputForm(@NotNull CmdLineInterfaceInputDialog parent, @NotNull CmdLineInterface cmdLineInterface, @NotNull Set<String> usedNames) {
        super(parent);
        this.cmdLineInterface = cmdLineInterface;
        this.usedNames = usedNames;

        initHintPanel();

        initDatabaseTypeField();
        initExecutableField();
        initNameField();
    }

    private void initHintPanel() {
        TextContent hintText = TextContent.plain(
                txt("msg.execution.hint.CommandLineInterfaceInput"));
        DBNHintForm hintForm = new DBNHintForm(this, hintText, null, true);
        hintPanel.add(hintForm.getComponent(), BorderLayout.CENTER);
    }

    private void initDatabaseTypeField() {
        DatabaseType databaseType = cmdLineInterface.getDatabaseType();
        databaseTypeComboBox.setValues(DatabaseType.nativelySupported());
        databaseTypeComboBox.setSelectedValue(databaseType);
        databaseTypeComboBox.addListener((o, n) -> initNameField());
    }

    private void initExecutableField() {
        DatabaseType databaseType = cmdLineInterface.getDatabaseType();

        String executablePath = cmdLineInterface.getExecutablePath();
        executableTextField.setText(executablePath);

        CmdLineInterface defaultClient = CmdLineInterface.getDefault(databaseType);
        String extension = OS.isWindows() ? ".exe" : "";
        addSingleFileChooser(
                getProject(), executableTextField,
                txt("cfg.execution.title.SelectCommandLineClient"),
                txt("cfg.execution.text.SelectCommandLineClient", defaultClient.getExecutablePath() + extension));
    }

    private void initNameField() {
        DatabaseType databaseType = databaseTypeComboBox.getSelectedValue();
        CmdLineInterface defaultClient = CmdLineInterface.getDefault(databaseType);
        String name = defaultClient.getName();

        name = nextNumberedIdentifier(name, true, () -> usedNames).trim();
        nameTextField.setText(name);
    }

    protected void initValidation() {
        addTextValidation(nameTextField, t -> isNotEmpty(t), txt("cfg.execution.error.InterfaceNameRequired"));
        addTextValidation(nameTextField, t -> isNotUsed(t), txt("cfg.execution.error.InterfaceNameAlreadyInUse"));

        JTextField execTextField = executableTextField.getTextField();
        addTextValidation(execTextField, t -> Strings.isNotEmpty(t), txt("cfg.execution.error.CommandLineExecutableRequired"));
        addTextValidation(execTextField, t -> new File(t).isFile(), txt("msg.shared.error.FileDoesNotExist"));
        addTextValidation(execTextField, t -> isMatchingDatabaseType(t), txt("cfg.execution.error.CommandLineExecutableDatabaseTypeMismatch"));
    }

    private boolean isMatchingDatabaseType(String filePath) {
        DatabaseType databaseType = CmdLineInterface.resolveDatabaseType(filePath);
        if (databaseType == DatabaseType.GENERIC) return true; // wave, could not clearly resolve database type
        return databaseType == databaseTypeComboBox.getSelectedValue();
    }

    private boolean isNotUsed(String name) {
        return !usedNames.contains(trim(name));
    }

    @NotNull
    @Override
    public JPanel getMainComponent() {
        return mainPanel;
    }

    public void applyFormChanges() {
        cmdLineInterface.setExecutablePath(getText(executableTextField));
        cmdLineInterface.setDatabaseType(databaseTypeComboBox.getSelectedValue());
        cmdLineInterface.setName(getText(nameTextField));
    }
}
