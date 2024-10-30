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

import com.dbn.assistant.entity.Profile;
import com.dbn.connection.ConnectionHandler;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.FileWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;

/**
 * Profile maintenance service implementation
 *
 * @author Emmanuel Jannetti (Oracle)
 */
@Slf4j
public class AIProfileServiceImpl extends AIServiceBase implements AIProfileService {

  public AIProfileServiceImpl(ConnectionHandler connection) {
      super(connection);
  }

    private void dumpThem(List<Profile> profileList, String path) {
      if (path != null ) {
          try {
              FileWriter writer = new FileWriter(path);
              new Gson().toJson(profileList, writer);
              writer.close();
          } catch (Exception e) {
              // ignore this
              if (log.isTraceEnabled())
                  log.trace("cannot dump profile " +e.getMessage());
          }
      }
  }

    @Override
    public CompletableFuture<Profile> get(String uuid) {
        assert false:"implement this !";
        return null;
    }

    @Override
  public CompletableFuture<List<Profile>> list()  {
    return CompletableFuture.supplyAsync(() -> {
      try {
        log.debug("getting profiles");
        List<Profile> profileList = executeCall(connection -> getAssistantInterface().listProfiles(connection));
        dumpThem(profileList, System.getProperty("fake.services.profiles.dump") );

        if (log.isDebugEnabled())
          log.debug("fetched profiles:" + profileList);
          ArrayList<Profile> profiles = new ArrayList<>(profileList);
          profiles.sort(Comparator.comparing(p -> p.getProfileName(), String.CASE_INSENSITIVE_ORDER));
          return profiles;
      } catch (SQLException e) {
        log.warn("error getting profiles", e);
        throw new CompletionException("Cannot get profiles", e);
      }
    });
  }


  @Override
  public CompletableFuture<Void> delete(String profileName) {
    return CompletableFuture.runAsync(() -> {
      try {
        executeTask(connection -> getAssistantInterface().dropProfile(connection, profileName));
      } catch (SQLException e) {
        log.warn("error deleting profile "+ profileName, e);
        throw new CompletionException("Cannot delete profile", e);
      }
    });

  }

  @Override
  public CompletionStage<Void> create(Profile profile) {
    return CompletableFuture.runAsync(() -> {
          try {
            executeTask(connection -> getAssistantInterface().createProfile(connection, profile));
          } catch (SQLException e) {
            log.warn("error creating profile", e);
            throw new CompletionException("Cannot create profile", e);
          }
        }
    );
  }

    @Override
  public CompletionStage<Void> update(Profile updatedProfile) {
    return CompletableFuture.runAsync(() -> {
          try {
            executeTask(connection -> getAssistantInterface().setProfileAttributes(connection, updatedProfile));
          } catch (SQLException e) {
            log.warn("error updating profiles", e);
            throw new CompletionException("Cannot update profile", e);
          }
        }
    );
  }

    @Override
    public void reset() {
        // no internal state to be reset
    }
}
