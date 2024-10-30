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

package com.dbn.assistant.service;


import com.dbn.assistant.DatabaseAssistantManager;
import com.dbn.assistant.entity.Credential;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.object.type.DBObjectType;
import com.intellij.openapi.project.Project;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


/**
 * AI profile credential service
 *
 * @author Ayoub Aarrasse (Oracle)
 */
public interface AICredentialService extends ManagedObjectService<Credential>{
  /**
   * Asynchronously creates a new credential.
   */
  CompletionStage<Void> create(Credential credential);

  /**
   * Asynchronously updates an attributes of existing credential.
   */
  CompletionStage<Void> update(Credential editedCredential);

  /**
   * Asynchronously lists detailed credential information from the database.
   */
  CompletableFuture<List<Credential>> list();

  /**
   * Asynchronously deletes a specific credential information from the database.
   */
  CompletableFuture<Void> delete(String credentialName);

  /**
   * Asynchronously updates the status (enabled/disabled) of the credential from the database.
   */
  void updateStatus(String credentialName, Boolean isEnabled);

  @Override
  default DBObjectType getObjectType() {
    return DBObjectType.CREDENTIAL;
  }


  class CachedProxy extends ManagedObjectServiceProxy<Credential> implements AICredentialService {
    public CachedProxy(AICredentialService backend) {
      super(backend);
    }

    @Override
    public void updateStatus(String credentialName, Boolean isEnabled) {
      AICredentialService delegate = (AICredentialService) getDelegate();
      delegate.updateStatus(credentialName, isEnabled);

    }
  }


  static AICredentialService getInstance(ConnectionHandler connection) {
    Project project = connection.getProject();
    ConnectionId connectionId = connection.getConnectionId();
    DatabaseAssistantManager manager = DatabaseAssistantManager.getInstance(project);
    return manager.getCredentialService(connectionId);
  }
}
