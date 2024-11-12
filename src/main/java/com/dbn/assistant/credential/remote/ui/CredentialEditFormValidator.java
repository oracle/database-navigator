/*
 * Copyright (c) 2024, Oracle and/or its affiliates.
 *
 * This software is dual-licensed to you under the Universal Permissive License
 * (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl or Apache License
 * 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
 * either license.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package com.dbn.assistant.credential.remote.ui;

import com.dbn.common.ref.WeakRef;
import com.dbn.nls.NlsSupport;
import com.dbn.object.type.DBCredentialType;
import com.intellij.openapi.ui.ValidationInfo;

import javax.swing.*;

/**
 * Validator for Credential editors
 * (code isolated from {@link CredentialEditForm})
 *
 * @author @author Ayoub Aarrasse (Oracle)
 */
public class CredentialEditFormValidator implements NlsSupport {

    private final WeakRef<CredentialEditForm> form;

    public CredentialEditFormValidator(CredentialEditForm form) {
        this.form = WeakRef.of(form);
    }

    private CredentialEditForm getForm() {
        return form.ensure();
    }

    public ValidationInfo validate() {
        CredentialEditForm form = getForm();

        JTextField credentialNameField = form.getCredentialNameField();
        String credentialName = credentialNameField.getText();
        if (credentialName.isEmpty()) {
            return new ValidationInfo(txt("ai.settings.credentials.info.credential_name.validation_error_1"), credentialNameField);
        }
        if (form.getUsedCredentialNames().contains(credentialName.toUpperCase()) && form.getCredential() == null) {
            return new ValidationInfo(txt("ai.settings.credentials.info.credential_name.validation_error_2"),
                    credentialNameField);
        }
        if (credentialNameField.isEnabled()) {
            DBCredentialType credentialType = (DBCredentialType) form.getCredentialTypeComboBox().getSelectedItem();
            if (credentialType == null) return new ValidationInfo("Please select a credential type"); // TODO NLS
            switch (credentialType) {
                case PASSWORD:
                    return validatePasswordCredential();
                case OCI:
                    return validateOciCredential();
            }
        }

        return null;
    }

    private ValidationInfo validateOciCredential() {
        CredentialEditForm form = getForm();

        JTextField userOcidField = form.getOciCredentialUserOcidField();
        String userOcid = userOcidField.getText();
        if (userOcid.isEmpty()) {
            return new ValidationInfo(txt("ai.settings.credentials.oci.info.user_ocid.validation_error_1"), userOcidField);
        }
        if (!userOcid.startsWith("ocid1.user.oc1.")) {
            return new ValidationInfo(txt("ai.settings.credentials.oci.info.user_ocid.validation_error_2"), userOcidField);
        }
        JTextField userTenancyOcidField = form.getOciCredentialUserTenancyOcidField();
        String userTenancyOcid = userTenancyOcidField.getText();
        if (userTenancyOcid.isEmpty()) {
            return new ValidationInfo(txt("ai.settings.credentials.oci.info.tenancy_ocid.validation_error_1"), userTenancyOcidField);
        }
        if (!userTenancyOcid.startsWith("ocid1.tenancy.oc1.")) {
            return new ValidationInfo(txt("ai.settings.credentials.oci.info.tenancy_ocid.validation_error_2"), userTenancyOcidField);
        }
        JTextField fingerprintField = form.getOciCredentialFingerprintField();
        if (fingerprintField.getText().isEmpty()) {
            return new ValidationInfo(txt("ai.settings.credentials.oci.info.fingerprint.validation_error_1"), fingerprintField);
        }
        JTextField privateKeyField = form.getOciCredentialPrivateKeyField();
        if (privateKeyField.getText().isEmpty()) {
            return new ValidationInfo(txt("ai.settings.credentials.oci.info.private_key.validation_error_1"), privateKeyField);
        }
        return null;
    }

    private ValidationInfo validatePasswordCredential() {
        CredentialEditForm form = getForm();

        JTextField usernameField = form.getPasswordCredentialUsernameField();
        if (usernameField.getText().isEmpty()) {
            return new ValidationInfo(txt("ai.settings.credentials.info.username.validation_error_1"), usernameField);
        }
        JPasswordField passwordField = form.getPasswordCredentialPasswordField();
        if (passwordField.getText().isEmpty()) {
            return new ValidationInfo(txt("ai.settings.credentials.info.password.validation_error_1"), passwordField);
        }
        return null;
    }
}
