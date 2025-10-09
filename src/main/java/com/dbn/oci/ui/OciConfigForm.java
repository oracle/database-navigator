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

package com.dbn.oci.ui;

import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.oci.util.OciConfigFileUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.io.File;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.common.util.Strings.isNotEmpty;

public class OciConfigForm extends DBNFormBase {
    private JPanel mainPanel;
    private TextFieldWithBrowseButton ociConfigFileTextField;
    private DBNComboBox<String> ociConfigProfileComboBox;
    private JBTextField ociCompartmentIdTextField;

    private final AssistantCredential credential;

    public OciConfigForm(@Nullable Disposable parent, AssistantCredential credential) {
        super(parent);
        this.credential = credential;

        addSingleFileChooser(getProject(), ociConfigFileTextField, "Select OCI configuration file", "");
        ociCompartmentIdTextField.getEmptyText().setText("ocid1.compartment.oc1..");
        onTextChange(ociConfigFileTextField, e -> ociConfigProfileComboBox.reloadValues());
    }

    @Override
    protected void initValidation() {
        addTextValidation(ociConfigFileTextField.getTextField(), s -> isNotEmpty(s), "Please select an OCI configuration file");
        addTextValidation(ociConfigFileTextField.getTextField(), s -> new File(s).isFile(), "Please select a valid OCI configuration file");
        addSelectionValidation(ociConfigProfileComboBox, "Please select an OCI configuration profile");
        addTextValidation(ociCompartmentIdTextField, s -> isNotEmpty(s), "Please provide an OCI Compartment ID");
    }

    public void applyFormChanges() {
        credential.setOciConfigFile(getText(ociConfigFileTextField));
        credential.setOciConfigProfile(getSelection(ociConfigProfileComboBox));
        credential.setOciCompartmentId(getText(ociCompartmentIdTextField));
    }

    public void resetFormChanges() {
        String ociConfigFile = credential.getOciConfigFile();
        String ociConfigProfile = credential.getOciConfigProfile();
        String ociCompartmentId = credential.getOciCompartmentId();

        setText(ociConfigFileTextField, ociConfigFile);
        setSelection(ociConfigProfileComboBox, ociConfigProfile);
        setText(ociCompartmentIdTextField, ociCompartmentId);

        ociConfigProfileComboBox.init(() -> loadOciConfigProfiles(), p -> Objects.equals(p, ociConfigProfile));
    }

    private List<String> loadOciConfigProfiles() {
        String configFilePath = getText(ociConfigFileTextField);
        return OciConfigFileUtil.getConfigProfileNames(configFilePath);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
