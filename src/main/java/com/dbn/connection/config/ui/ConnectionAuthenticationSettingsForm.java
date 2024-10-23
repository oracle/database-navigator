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

package com.dbn.connection.config.ui;

import com.dbn.common.database.AuthenticationInfo;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.connection.config.ConnectionDatabaseSettings;
import com.dbn.connection.ui.ConnectionAuthenticationFieldsForm;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Wrapper for the {@link ConnectionAuthenticationFieldsForm} to be used in connection settings
 */
public class ConnectionAuthenticationSettingsForm extends DBNFormBase {
    private JPanel mainPanel;

    private final ConnectionAuthenticationFieldsForm fieldsForm = new ConnectionAuthenticationFieldsForm(this);

    public ConnectionAuthenticationSettingsForm(@NotNull ConnectionDatabaseSettingsForm parentComponent) {
        super(parentComponent);
        mainPanel.add(fieldsForm.getComponent());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }

    public String getUser() {
        return fieldsForm.getUser();
    }

    public String getTokenConfigFile() {
        return fieldsForm.getTokenConfigFile();
    }

    public String getTokenProfile() {
        return fieldsForm.getTokenProfile();
    }

    public void resetFormChanges() {
        ConnectionDatabaseSettingsForm parent = ensureParentComponent();
        ConnectionDatabaseSettings configuration = parent.getConfiguration();
        AuthenticationInfo authenticationInfo = configuration.getAuthenticationInfo();

        fieldsForm.resetFormChanges(authenticationInfo);
    }

    public void applyFormChanges(AuthenticationInfo authenticationInfo) {
        fieldsForm.applyFormChanges(authenticationInfo);
    }
}
