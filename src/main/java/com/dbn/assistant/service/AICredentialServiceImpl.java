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

import com.dbn.assistant.entity.Credential;
import com.dbn.connection.ConnectionHandler;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Service class responsible for managing AI credentials.
 * Provides functionality to asynchronously list detailed information about credentials stored in the database.
 *
 * @author Emmanuel Jannetti (Oracle)
 */
@Slf4j
public class AICredentialServiceImpl extends AIServiceBase implements AICredentialService {

  /**
   * Constructs a new AICredentialService with a specified connection handler.
   *
   * @param connection The {@link ConnectionHandler} responsible for managing database connections and interactions.
   */
  public AICredentialServiceImpl(ConnectionHandler connection) {
    super(connection);
  }

  @Override
  public CompletableFuture<Void> create(Credential credential) {
    return CompletableFuture.runAsync(() -> {
      try {
        executeTask(connection -> getAssistantInterface().createCredential(connection, credential));
      } catch (SQLException e) {
        throw new CompletionException("Cannot create credential", e);
      }
    });
  }

  @Override
  public CompletableFuture<Void> update(Credential editedCredential) {
    return CompletableFuture.runAsync(() -> {
      try {
        executeTask(connection -> getAssistantInterface().setCredentialAttribute(connection, editedCredential));
      } catch (SQLException e) {
        throw new CompletionException("Cannot update credential", e);
      }
    });
  }

  @Override
  public void reset() {
    // no state to reset
  }

  @Override
  public CompletableFuture<List<Credential>> list() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        List<Credential> credentialList = executeCall(connection-> getAssistantInterface().listCredentials(connection));
        if (System.getProperty("fake.services.credentials.dump") != null) {
          try {
            FileWriter writer = new FileWriter(System.getProperty("fake.services.credentials.dump"));
            new Gson().toJson(credentialList, writer);
            writer.close();
          } catch (Exception e) {
            // ignore this
            if (log.isTraceEnabled())
              log.trace("cannot dump profile " + e.getMessage());
          }
        }
        // Fetch and return detailed list of credentials
        return credentialList;
      } catch (SQLException e) {
        throw new CompletionException("Cannot list credentials", e);
      }
    });
  }

  @Override
  public CompletableFuture<Credential> get(String uuid) {
    throw new UnsupportedOperationException("Not supported yet.");
  }

  @Override
  public CompletableFuture<Void> delete(String credentialName) {
    if (credentialName.isEmpty()) {
      throw new IllegalArgumentException("Credential Name shouldn't be empty");
    }
    return CompletableFuture.runAsync(() -> {
      try {
        executeTask(connection-> getAssistantInterface().dropCredential(connection, credentialName));
      } catch (SQLException e) {
        throw new CompletionException("Cannot delete credential", e);
      }
    });
  }

  @Override
  public void updateStatus(String credentialName, Boolean isEnabled) {
    if (credentialName.isEmpty()) {
      throw new IllegalArgumentException("Credential Name shouldn't be empty");
    }
    CompletableFuture.runAsync(() -> {
      try {
        executeTask(connection-> getAssistantInterface().updateCredentialStatus(connection, credentialName, isEnabled));
      } catch (SQLException e) {
        throw new CompletionException("Cannot update credential status", e);
      }
    });
  }
}
