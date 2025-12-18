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

package com.dbn.oci.config.ui;

import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.oci.config.OciConfig;
import com.dbn.oci.config.OciConfigFileUtil;
import com.dbn.oci.config.OciConfigType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.io.File;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.ui.form.field.JComponentFilter.array;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.initComboBox;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.Commons.nvl;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.common.util.FileChoosers.extensionFilter;
import static com.dbn.common.util.Strings.isNotEmpty;

public class OciConfigForm extends DBNFormBase {
    private JPanel mainPanel;
    private TextFieldWithBrowseButton configFileTextField;
    private TextFieldWithBrowseButton privateKeyFileTextField;
    private JBTextField userIdTextField;
    private JBTextField compartmentIdTextField;
    private JBTextField tenancyIdTextField;
    private JBTextField fingerprintTextField;
    private DBNComboBox<String> configProfileComboBox;
    private JComboBox<OciConfigType> configTypeComboBox;
    private JLabel configFileLabel;
    private JLabel configProfileLabel;
    private JLabel userIdLabel;
    private JLabel tenancyIdLabel;
    private JLabel compartmentIdLabel;
    private JLabel privateKeyFileLabel;
    private JLabel fingerprintLabel;

    private final OciConfig config;

    public OciConfigForm(@Nullable Disposable parent, OciConfig config) {
        super(parent);
        this.config = config;

        initComboBox(configTypeComboBox, OciConfigType.values());
        addSingleFileChooser(getProject(), configFileTextField, "Select OCI configuration file", "");
        addSingleFileChooser(getProject(), privateKeyFileTextField, "Select OCI configuration file", "").withFileFilter(extensionFilter("pem"));

        userIdTextField.getEmptyText().setText("ocid1.user.oc1..");
        tenancyIdTextField.getEmptyText().setText("ocid1.tenancy.oc1..");
        compartmentIdTextField.getEmptyText().setText("ocid1.compartment.oc1..");
        onTextChange(configFileTextField, e -> configProfileComboBox.reloadValues());
        onSelectionChange(configTypeComboBox, v -> updateFieldAvailability());
    }

    @Override
    protected void initValidation() {
        addTextValidation(configFileTextField.getTextField(), s -> isNotEmpty(s), "Please select a Configuration file");
        addTextValidation(configFileTextField.getTextField(), s -> new File(s).isFile(), "Please select a valid Configuration file");
        addSelectionValidation(configProfileComboBox, "Please select an OCI configuration profile");

        addTextValidation(compartmentIdTextField, s -> isNotEmpty(s), "Please provide an Compartment ID");
        addTextValidation(compartmentIdTextField, s -> s.startsWith("ocid1.compartment.oc1.."), "Please provide a valid Compartment ID");

        addTextValidation(userIdTextField, s -> isNotEmpty(s), "Please provide a User ID");
        addTextValidation(userIdTextField, s -> s.startsWith("ocid1.user.oc1.."), "Please provide a valid User ID");

        addTextValidation(tenancyIdTextField, s -> isNotEmpty(s), "Please provide a Tenancy ID");
        addTextValidation(tenancyIdTextField, s -> s.startsWith("ocid1.tenancy.oc1.."), "Please provide a valid Tenancy ID");

        addTextValidation(configFileTextField.getTextField(), s -> isNotEmpty(s), "Please select a Private key file");
        addTextValidation(configFileTextField.getTextField(), s -> new File(s).isFile(), "Please select a valid Private key file");

        addTextValidation(fingerprintTextField, s -> isNotEmpty(s), "Please provide a Fingerprint");

    }

    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();
        fieldAdapter.initFieldsVisibility(() -> getConfigType() == OciConfigType.FILE, array(
                configFileLabel,
                configFileTextField,
                configProfileLabel,
                configProfileComboBox));

        fieldAdapter.initFieldsVisibility(() -> getConfigType() == OciConfigType.CUSTOM, array(
                userIdLabel,
                userIdTextField,
                tenancyIdLabel,
                tenancyIdTextField,
                privateKeyFileLabel,
                privateKeyFileTextField,
                fingerprintLabel,
                fingerprintTextField));
    }

    private OciConfigType getConfigType() {
        return nvl(getSelection(configTypeComboBox), config.getType());
    }

    public void applyFormChanges() {
        config.setType(getSelection(configTypeComboBox));

        config.setUserId(getText(userIdTextField));
        config.setTenancyId(getText(tenancyIdTextField));
        config.setCompartmentId(getText(compartmentIdTextField));

        config.setConfigFile(getText(configFileTextField));
        config.setConfigProfile(getSelection(configProfileComboBox));

        config.setPrivateKeyFile(getText(privateKeyFileTextField));
        config.setFingerprint(getText(fingerprintTextField));
    }

    public void resetFormChanges() {
        String configProfile = config.getConfigProfile();
        setSelection(configTypeComboBox, config.getType());

        setText(userIdTextField, config.getUserId());
        setText(tenancyIdTextField, config.getTenancyId());
        setText(compartmentIdTextField, config.getCompartmentId());

        setText(privateKeyFileTextField, config.getPrivateKeyFile());
        setText(fingerprintTextField, config.getFingerprint());

        setText(configFileTextField, config.getConfigFile());
        setSelection(configProfileComboBox, configProfile);

        configProfileComboBox.initialize(() -> loadOciConfigProfiles(), p -> Objects.equals(p, configProfile));
    }

    private List<String> loadOciConfigProfiles() {
        String configFilePath = getText(configFileTextField);
        return OciConfigFileUtil.getConfigProfileNames(configFilePath);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
