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

package com.dbn.assistant.settings.ui;

import com.dbn.assistant.credential.remote.ui.CredentialManagementForm;
import com.dbn.assistant.profile.ui.ProfileManagementForm;
import com.dbn.common.ui.dialog.DBNDialog;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.form.DBNHeaderForm;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionRef;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class AssistantDatabaseConfigForm extends DBNFormBase {
    private JPanel mainPanel;
    private JPanel headerPanel;
    private JTabbedPane settingsTabbedPane;
    private JPanel credentialsPanel;
    private JPanel profilesPanel;

    private final ConnectionRef connection;

    public AssistantDatabaseConfigForm(@Nullable DBNDialog<?> parent, ConnectionHandler connection) {
        super(parent);
        this.connection = ConnectionRef.of(connection);

        initHeaderPanel();
        initConfigTabs();
    }

    private void initHeaderPanel() {
        ConnectionHandler connection = getConnection();
        DBNHeaderForm headerForm = new DBNHeaderForm(this, connection);
        headerPanel.add(headerForm.getComponent(), BorderLayout.CENTER);
    }

    private void initConfigTabs() {
        ConnectionHandler connection = getConnection();
        ProfileManagementForm profileManagementForm = new ProfileManagementForm(this, connection);
        CredentialManagementForm credentialManagementForm = new CredentialManagementForm(this, connection);

        profilesPanel.add(profileManagementForm.getComponent());
        credentialsPanel.add(credentialManagementForm.getComponent());
    }

    private ConnectionHandler getConnection() {
        return connection.ensure();
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
