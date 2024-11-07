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

package com.dbn.assistant.profile.wizard;

import com.dbn.assistant.provider.AIModel;
import com.dbn.assistant.provider.AIProvider;
import com.dbn.object.DBAIProfile;
import com.dbn.object.common.DBObject;
import com.dbn.object.lookup.DBObjectRef;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ProfileData {
    private String name;
    private String description;

    private AIProvider provider;
    private AIModel model;

    private String credentialName;
    private List<DBObjectRef<DBObject>> objectList = new ArrayList<>();

    private double temperature;
    private boolean enabled = true;


    public ProfileData() {

    }

    public ProfileData(DBAIProfile profile) {
        if (profile == null) return;

        this.name = profile.getName();
        this.description = profile.getDescription();
        this.provider = profile.getProvider();
        this.model = profile.getModel();
        this.credentialName = profile.getCredentialName();
        this.enabled = profile.isEnabled();
        this.temperature = profile.getTemperature();
        this.objectList = DBObjectRef.from(profile.getObjects());
    }
}
