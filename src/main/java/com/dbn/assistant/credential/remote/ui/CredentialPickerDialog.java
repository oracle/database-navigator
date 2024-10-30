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

import com.dbn.assistant.credential.local.LocalCredential;
import com.dbn.common.routine.Consumer;
import com.dbn.common.ui.dialog.DBNDialog;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class CredentialPickerDialog extends DBNDialog<CredentialPickerForm> {
  private final Consumer<LocalCredential> callback;

  public CredentialPickerDialog(Project project, Consumer<LocalCredential> callback) {
    super(project, "Credential Templates", true);

    Action okAction = getOKAction();
    renameAction(okAction, "Select");
    okAction.setEnabled(false);
    this.callback = callback;
    init();
  }

  /**
   * Defines the behaviour when we click the create/update button
   * It starts by validating, and then it executes the specifies action
   */
  @Override
  protected void doOKAction() {
    callback.accept(getSelectedCredential());
    super.doOKAction();
  }

  @Override
  protected Action @NotNull [] createActions() {
    return new Action[]{getOKAction(), getCancelAction()};
  }

  @Override
  protected @NotNull CredentialPickerForm createForm() {
    return new CredentialPickerForm(this);
  }


  public void selectionChanged() {
    getOKAction().setEnabled(getSelectedCredential() != null);
  }

  private @Nullable LocalCredential getSelectedCredential() {
    return getForm().getSelectedCredential();
  }
}
