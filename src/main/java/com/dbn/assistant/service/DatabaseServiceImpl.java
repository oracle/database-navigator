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

import com.dbn.assistant.entity.DBObjectItem;
import com.dbn.connection.ConnectionHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Service to handle DB objects and operations
 *
 * @author Emmanuel Jannetti (Oracle)
 */
@Slf4j
public class DatabaseServiceImpl extends AIServiceBase implements DatabaseService {


  public DatabaseServiceImpl(ConnectionHandler connection) {
    super(connection);
  }

  public CompletableFuture<List<String>> getSchemaNames() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.debug("fetching schemas");
        List<String> schemas = executeCall(connection -> getAssistantInterface().listSchemas(connection));
        if (log.isDebugEnabled())
          log.debug("fetched schemas: " + schemas);
        if (System.getProperty("fake.services.schemas.dump") != null) {
          try {
            FileWriter writer = new FileWriter(System.getProperty("fake.services.schemas.dump"));
            new Gson().toJson(schemas, writer);
            writer.close();
          } catch (Exception e) {
            // ignore this
            if (log.isTraceEnabled())
              log.trace("cannot dump schemas " + e.getMessage());
          }
        }
        return schemas;
      } catch (SQLException e) {
        log.warn("cannot fetch schemas", e);
        throw new CompletionException("Cannot get schemas", e);
      }
    });
  }

  public CompletableFuture<List<DBObjectItem>> getObjectItemsForSchema(String schema) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.debug("fetching objects for schema " + schema);
        List<DBObjectItem> objectListItemsList = executeCall(connection -> getAssistantInterface().listObjectListItems(connection, schema));
        log.debug("getObjectItemsForSchema: "+objectListItemsList.size() + " objects returned ");
        if (System.getProperty("fake.services.dbitems.dump") != null) {
          try {
            FileWriter writer = new FileWriter(System.getProperty("fake.services.dbitems.dump"), true);
            writer.write(schema);
            writer.write(':');
            new GsonBuilder().setLenient().create().toJson(objectListItemsList, writer);
            writer.write('\n');
            writer.close();
          } catch (Exception e) {
            // ignore this
            if (log.isTraceEnabled())
              log.trace("Cannot dump obj list" + e.getMessage());
          }
        }
        return objectListItemsList;
      } catch (SQLException e) {
        log.warn("error while fetching schema object list", e);
        throw new CompletionException("Cannot list object list items", e);
      }
    });
  }

  public CompletableFuture<Void> grantACLRights(String command) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        executeTask(connection -> getAssistantInterface().grantACLRights(connection, command));
      } catch (SQLException e) {
        throw new CompletionException(e);
      }
      return null;
    });
  }

  public CompletableFuture<Void> grantPrivilege(String username) {
    return CompletableFuture.supplyAsync(() -> {
      try {
        executeTask(connection -> getAssistantInterface().grantPrivilege(connection, username));
      } catch (SQLException e) {
        throw new CompletionException(e);
      }
      return null;
    });
  }

  public CompletableFuture<Void> isUserAdmin() {
    return CompletableFuture.supplyAsync(() -> {
      try {
        executeTask(connection -> getAssistantInterface().checkAdmin(connection));
      } catch (SQLException e) {
        throw new CompletionException(e);
      }
      return null;
    });
  }
}
