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
import com.dbn.common.util.FileChoosers;
import com.dbn.oci.config.OciConfig;
import com.dbn.oci.config.OciConfigFileUtil;
import com.dbn.oci.config.OciConfigType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
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
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.nls.NlsResources.txt;
import static com.dbn.oci.util.OciIdentifiers.isCompartmentScopeOcid;
import static com.dbn.oci.util.OciIdentifiers.isTenancyOcid;
import static com.dbn.oci.util.OciIdentifiers.isUserOcid;

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
        initConfigFileChooser();
        initPrivateKeyFileChooser();

        userIdTextField.getEmptyText().setText("ocid1.user.oc1..");
        tenancyIdTextField.getEmptyText().setText("ocid1.tenancy.oc1..");
        compartmentIdTextField.getEmptyText().setText("ocid1.compartment.oc1.. / ocid1.tenancy.oc1..");
        onTextChange(configFileTextField, e -> configProfileComboBox.reloadValues());
        onSelectionChange(configTypeComboBox, v -> updateFieldAvailability());
    }

    private void initConfigFileChooser() {
        addSingleFileChooser(getProject(), configFileTextField, txt("cfg.oci.title.SelectConfigFile"), "");
    }

    private void initPrivateKeyFileChooser() {
        FileChooserDescriptor descriptor = addSingleFileChooser(getProject(), privateKeyFileTextField, txt("cfg.oci.title.SelectPrivateKeyFile"), "");
        //descriptor.withFileFilter(extensionFilter("pem"));
        FileChoosers.withExtensionFilter(descriptor, "pem");
    }

    @Override
    protected void initValidation() {
        addTextValidation(configFileTextField.getTextField(), s -> isNotEmpty(s), txt("cfg.oci.error.ConfigFileRequired"));
        addTextValidation(configFileTextField.getTextField(), s -> new File(s).isFile(), txt("cfg.oci.error.ValidConfigFileRequired"));
        addSelectionValidation(configProfileComboBox, txt("cfg.oci.error.ConfigProfileRequired"));

        addTextValidation(compartmentIdTextField, s -> isNotEmpty(s), txt("cfg.oci.error.CompartmentIdRequired"));
        addTextValidation(compartmentIdTextField, s -> isCompartmentScopeOcid(s), txt("cfg.oci.error.ValidCompartmentIdRequired"));

        addTextValidation(userIdTextField, s -> isNotEmpty(s), txt("cfg.oci.error.UserIdRequired"));
        addTextValidation(userIdTextField, s -> isUserOcid(s), txt("cfg.oci.error.ValidUserIdRequired"));

        addTextValidation(tenancyIdTextField, s -> isNotEmpty(s), txt("cfg.oci.error.TenancyIdRequired"));
        addTextValidation(tenancyIdTextField, s -> isTenancyOcid(s), txt("cfg.oci.error.ValidTenancyIdRequired"));

        addTextValidation(privateKeyFileTextField.getTextField(), s -> isNotEmpty(s), txt("cfg.oci.error.PrivateKeyFileRequired"));
        addTextValidation(privateKeyFileTextField.getTextField(), s -> new File(s).isFile(), txt("cfg.oci.error.ValidPrivateKeyFileRequired"));

        addTextValidation(fingerprintTextField, s -> isNotEmpty(s), txt("cfg.oci.error.FingerprintRequired"));

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

        configProfileComboBox
                .withValueLoader(() -> loadOciConfigProfiles())
                .withValuePreselector(p -> Objects.equals(p, configProfile))
                .triggerLoad();
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
