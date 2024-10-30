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

package com.dbn.assistant.credential.remote;

import com.dbn.DatabaseNavigator;
import com.dbn.assistant.credential.remote.adapter.*;
import com.dbn.common.component.PersistentState;
import com.dbn.common.component.ProjectComponentBase;
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.object.DBCredential;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.management.ObjectManagementAdapterBase;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.dbn.common.component.Components.projectService;

/**
 * Database Assistant credential management component
 * Exposes CRUD-like actions for the {@link DBCredential} entities
 * Internally instantiates the specialized {@link com.dbn.object.management.ObjectManagementAdapter}
 * component and invokes the MODAL invocation option of the adapter (TODO - should ask the caller to specify the invocation mode)
 *
 * @author Dan Cioca (Oracle)
 */
@Slf4j
@State(
    name = CredentialManagementService.COMPONENT_NAME,
    storages = @Storage(DatabaseNavigator.STORAGE_FILE))

public class CredentialManagementService extends ProjectComponentBase implements PersistentState {
  public static final String COMPONENT_NAME = "DBNavigator.Project.CredentialManagementService";

  private CredentialManagementService(Project project) {
    super(project, COMPONENT_NAME);
  }

  public static CredentialManagementService getInstance(@NotNull Project project) {
    return projectService(project, CredentialManagementService.class);
  }

  public void createCredential(DBCredential credential, OutcomeHandler successHandler) {
    invokeAdapter(credential, ObjectChangeAction.CREATE, successHandler);
  }

  public void updateCredential(DBCredential credential, OutcomeHandler successHandler) {
    invokeAdapter(credential, ObjectChangeAction.UPDATE, successHandler);
  }

  public void deleteCredential(DBCredential credential, OutcomeHandler successHandler) {
    invokeAdapter(credential, ObjectChangeAction.DELETE, successHandler);
  }

  public void enableCredential(DBCredential credential, OutcomeHandler successHandler) {
    invokeAdapter(credential, ObjectChangeAction.ENABLE, successHandler);
  }

  public void disableCredential(DBCredential credential, OutcomeHandler successHandler) {
    invokeAdapter(credential, ObjectChangeAction.DISABLE, successHandler);
  }

  private static void invokeAdapter(DBCredential credential, ObjectChangeAction action, OutcomeHandler successHandler) {
    ObjectManagementAdapterBase<DBCredential> adapter = createAdapter(credential, action);
    if (adapter == null) return;

    adapter.addOutcomeHandler(OutcomeType.SUCCESS, successHandler);
    adapter.invokeModal();
  }

  @Nullable
  private static ObjectManagementAdapterBase<DBCredential> createAdapter(DBCredential credential, ObjectChangeAction action) {
    switch (action) {
      case CREATE: return new CredentialCreationAdapter(credential);
      case UPDATE: return new CredentialUpdateAdapter(credential);
      case DELETE: return new CredentialDeleteAdapter(credential);
      case ENABLE: return new CredentialEnableAdapter(credential);
      case DISABLE: return new CredentialDisableAdapter(credential);
      default: return null;
    }
  }

  /*********************************************
   *            PersistentStateComponent       *
   *********************************************/

  @Override
  public Element getComponentState() {
    return null;
  }

  @Override
  public void loadComponentState(@NotNull Element element) {
  }
}
