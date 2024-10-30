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
import com.dbn.assistant.entity.DBObjectItem;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.intellij.openapi.project.Project;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Database information service
 *
 * @author Emmanuel Jannetti (Oracle)
 */
public interface DatabaseService {
    /**
     * Loads all schemas that are accessible for the current user asynchronously
     */
    CompletableFuture<List<String>> getSchemaNames();

    /**
     * Loads all object list items accessible to the user from a specific schema asynchronously
     */
    CompletableFuture<List<DBObjectItem>> getObjectItemsForSchema(String schema);

    CompletionStage<Void> grantACLRights(String command);

    CompletionStage<Void> grantPrivilege(String username);

    CompletionStage<Void> isUserAdmin();

    static DatabaseService getInstance(ConnectionHandler connection) {
        Project project = connection.getProject();
        ConnectionId connectionId = connection.getConnectionId();
        DatabaseAssistantManager manager = DatabaseAssistantManager.getInstance(project);
        return manager.getDatabaseService(connectionId);
    }

}
