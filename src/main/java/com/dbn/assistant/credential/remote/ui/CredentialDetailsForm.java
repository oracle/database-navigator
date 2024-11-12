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

import com.dbn.common.event.ProjectEvents;
import com.dbn.common.ui.form.DBNFormBase;
import com.dbn.common.ui.util.Borders;
import com.dbn.object.DBCredential;
import com.dbn.object.event.ObjectChangeListener;
import com.dbn.object.lookup.DBObjectRef;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.Set;

import static com.dbn.object.type.DBObjectType.AI_PROFILE;

public class CredentialDetailsForm extends DBNFormBase {
    private JPanel mainPanel;
    private JTextField credentialNameTextField;
    private JTextField userNameTextField;
    private JTextField commentsTextField;
    private JCheckBox enabledCheckBox;
    private JList<String> usageList;

    private final DBObjectRef<DBCredential> credential;

    public CredentialDetailsForm(@NotNull CredentialManagementForm parent, DBCredential credential) {
        super(parent);
        this.credential = DBObjectRef.of(credential);

        initCredentialFields();
        initCredentialUsageList();
        initChangeListener();
    }

    private void initChangeListener() {
        ProjectEvents.subscribe(ensureProject(), this, ObjectChangeListener.TOPIC, (connectionId, ownerId, objectType) -> {
            if (connectionId != credential.getConnectionId()) return;
            if (objectType != AI_PROFILE) return;

            refreshUsageListData();
        });
    }

    public DBCredential getCredential() {
        return credential.ensure();
    }

    private void initCredentialUsageList() {
        usageList.setBorder(Borders.EMPTY_BORDER);
        usageList.setCellRenderer(createListCellRenderer());
        refreshUsageListData();
    }

    private void refreshUsageListData() {
        String credentialName = credential.getObjectName();
        Set<String> usedByProfiles = getManagementForm().getCredentialUsage(credentialName);
        usageList.setListData(usedByProfiles.toArray(new String[0]));
    }

    private static @NotNull ColoredListCellRenderer<String> createListCellRenderer() {
        return new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends String> list, String value, int index, boolean selected, boolean hasFocus) {
                append(value, list.isEnabled() ?
                        SimpleTextAttributes.REGULAR_ATTRIBUTES :
                        SimpleTextAttributes.GRAY_ATTRIBUTES);
            }
        };
    }

    public CredentialManagementForm getManagementForm() {
        return super.getParentComponent();
    }

    private void initCredentialFields() {
        DBCredential credential = getCredential();
        credentialNameTextField.setText(credential.getName());
        userNameTextField.setText(credential.getUserName());
        commentsTextField.setText(credential.getComments());
        enabledCheckBox.setSelected(credential.isEnabled());
    }

    @Override
    protected JComponent getMainComponent() {
        return mainPanel;
    }
}
