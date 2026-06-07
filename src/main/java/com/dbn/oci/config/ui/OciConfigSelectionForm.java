/*
 * Copyright 2026 Oracle and/or its affiliates
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

import com.dbn.common.state.StateAttributes;
import com.dbn.common.thread.Dispatch;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.field.DBNFormFieldAdapter;
import com.dbn.common.ui.misc.DBNComboBox;
import com.dbn.oci.config.OciConfig;
import com.dbn.oci.config.OciConfigFileUtil;
import com.dbn.oci.config.OciConfigManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.ui.components.JBTextField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.io.File;
import java.util.List;
import java.util.Objects;

import static com.dbn.common.ui.form.DBNFormState.initPersistence;
import static com.dbn.common.ui.util.ComboBoxes.getSelection;
import static com.dbn.common.ui.util.ComboBoxes.onSelectionChange;
import static com.dbn.common.ui.util.ComboBoxes.setSelection;
import static com.dbn.common.ui.util.TextFields.getText;
import static com.dbn.common.ui.util.TextFields.onTextChange;
import static com.dbn.common.ui.util.TextFields.setText;
import static com.dbn.common.util.FileChoosers.addSingleFileChooser;
import static com.dbn.common.util.Strings.isNotEmpty;
import static com.dbn.nls.NlsResources.txt;

public class OciConfigSelectionForm extends DBNFormBase {
    private JPanel mainPanel;
    private TextFieldWithBrowseButton configFileTextField;
    private JBTextField privateKeyFileTextField;
    private JBTextField userIdTextField;
    private JBTextField tenancyIdTextField;
    private JBTextField fingerprintTextField;
    private DBNComboBox<String> configProfileComboBox;

    private final OciConfig config;

    public OciConfigSelectionForm(@Nullable Disposable parent, OciConfig config) {
        super(parent);
        this.config = config;

        addSingleFileChooser(getProject(), configFileTextField, txt("cfg.oci.title.SelectConfigFile"), "");

        userIdTextField.getEmptyText().setText(txt("cfg.oci.placeholder.UserOcidExample"));
        tenancyIdTextField.getEmptyText().setText(txt("cfg.oci.placeholder.TenancyOcidExample"));
        onTextChange(configFileTextField, e -> configProfileComboBox.reloadValues());
        onSelectionChange(configProfileComboBox, v -> updateConfigFieldValues());

        resetFormChanges();
    }

    protected void initStatePersistence() {
        Project project = ensureProject();
        OciConfigManager ociConfigManager = OciConfigManager.getInstance(project);
        StateAttributes state = ociConfigManager.getState("CONFIG_FILE_SELECTOR");

        initPersistence(configFileTextField, state, "config-file");
    }

    private void updateConfigFieldValues() {
        setText(userIdTextField, "");
        setText(tenancyIdTextField, "");
        setText(privateKeyFileTextField, "");
        setText(fingerprintTextField, "");

        Dispatch.async(mainPanel, () -> {
            String configFilePath = getConfigFilePath();
            String configProfile = getSelection(configProfileComboBox);
            return OciConfigFileUtil.getConfigProfileValues(configFilePath, configProfile);
        }, values -> {
            setText(userIdTextField, values.get("user"));
            setText(tenancyIdTextField, values.get("tenancy"));
            setText(privateKeyFileTextField, values.get("key_file"));
            setText(fingerprintTextField, values.get("fingerprint"));
        });
    }


    @Override
    protected void initValidation() {
        addTextValidation(configFileTextField.getTextField(), s -> isNotEmpty(s), txt("cfg.oci.error.ConfigFileRequired"));
        addTextValidation(configFileTextField.getTextField(), s -> new File(s).isFile(), txt("cfg.oci.error.ValidConfigFileRequired"));
        addSelectionValidation(configProfileComboBox, txt("cfg.oci.error.ConfigProfileRequired"));
    }

    protected void initFieldAvailability() {
        DBNFormFieldAdapter fieldAdapter = getFieldAdapter();

    }

    public void applyFormChanges() {
        config.setUserId(getText(userIdTextField));
        config.setTenancyId(getText(tenancyIdTextField));

        config.setConfigFile(getConfigFilePath());
        config.setConfigProfile(getSelection(configProfileComboBox));

        config.setPrivateKeyFile(getText(privateKeyFileTextField));
        config.setFingerprint(getText(fingerprintTextField));
    }

    public void resetFormChanges() {
        String configProfile = config.getConfigProfile();

        setText(userIdTextField, config.getUserId());
        setText(tenancyIdTextField, config.getTenancyId());

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
        String configFilePath = getConfigFilePath();
        return OciConfigFileUtil.getConfigProfileNames(configFilePath);
    }

    private @NotNull String getConfigFilePath() {
        return getText(configFileTextField);
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
