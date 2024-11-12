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

package com.dbn.assistant.credential.remote.action;

import com.dbn.assistant.credential.remote.ui.CredentialManagementForm;
import com.dbn.common.action.DataKeys;
import com.dbn.common.action.ProjectAction;
import com.dbn.object.DBCredential;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generic stub for actions related to management of credentials
 * (features the lookup of the credential management form from the context)
 *
 * @author Dan Cioca (Oracle)
 */
public abstract class CredentialManagementAction extends ProjectAction {

    @Nullable
    protected static CredentialManagementForm getManagementForm(@NotNull AnActionEvent e) {
        return e.getData(DataKeys.CREDENTIAL_MANAGEMENT_FORM);
    }

    @Nullable
    protected static DBCredential getSelectedCredential(@NotNull AnActionEvent e) {
        CredentialManagementForm managementForm = getManagementForm(e);
        if (managementForm == null) return null;

        return managementForm.getSelectedCredential();
    }
}
