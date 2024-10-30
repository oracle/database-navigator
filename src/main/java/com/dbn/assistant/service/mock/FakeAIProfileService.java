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

package com.dbn.assistant.service.mock;

import com.dbn.assistant.entity.Profile;
import com.dbn.assistant.service.AIProfileService;
import com.dbn.connection.ConnectionId;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Mock profile maintenance service
 *
 * @author Emmanuel Jannetti (Oracle)
 */
public class FakeAIProfileService implements AIProfileService {

    Type PROFILE_TYPE = new TypeToken<List<Profile>>() {
    }.getType();
    String profilesRepoFilename = System.getProperty("fake.services.profile.dump", "/var/tmp/profiles.json");
    //keep track of profiles
    // no synch needed
    List<Profile> profiles = null;

    @Override
    public CompletableFuture<Profile> get(String uuid) {
        assert false:"implement this !";
        return null;
    }

    @Override
    public CompletableFuture<List<Profile>> list() {
        if (profiles == null) {
            try {
                this.profiles = new Gson().fromJson(new FileReader(profilesRepoFilename), PROFILE_TYPE);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("cannot read profile list " + e.getMessage());
            }
        }
        return CompletableFuture.completedFuture(this.profiles);
    }


    @Override
    public CompletableFuture<Void> delete(String profileName) {
        this.profiles.removeIf(profile -> profile.getProfileName().equalsIgnoreCase(profileName));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> create(Profile profile) {
        this.profiles.add(profile);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletionStage<Void> update(Profile updatedProfile) {
        this.profiles.removeIf(p -> p.getProfileName().equalsIgnoreCase(updatedProfile.getProfileName()));
        this.profiles.add(updatedProfile);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void reset() {

    }

    @Override
    public ConnectionId getConnectionId() {
        return null;
    }

}
