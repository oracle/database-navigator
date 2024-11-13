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

package com.dbn.driver.packages;

import lombok.Getter;
import java.util.List;

/**
 * Library holds the metadata for a Maven dependency required by a driver package.
 * The information includes groupId, artifactId, and version.
 * <p>
 * Example:
 * <pre>
 * {@code
 * <library group-id="javax.resource" artifact-id="connector-api" version="1.5"/>
 * }
 * </pre>
 *
 * @author Ayoub Aarrasse
 */
@Getter
public class Library {
    private final String groupId;
    private final String artifactId;
    private final String version;
    private final List<Developer> developers;
    private final List<License> licenses;

    public Library(String groupId, String artifactId, String version, List<Developer> developers, List<License> licenses) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.developers = developers;
        this.licenses = licenses;
    }

    @Override
    public String toString() {
        return String.format("Library [groupId=%s, artifactId=%s, version=%s]", groupId, artifactId, version);
    }
}