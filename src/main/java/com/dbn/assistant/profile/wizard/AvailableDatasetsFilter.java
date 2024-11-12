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

import com.dbn.common.filter.Filter;
import com.dbn.common.util.Strings;
import com.dbn.object.DBDataset;
import com.dbn.object.type.DBObjectType;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data  // hasCode is important here to act as filter signature for refreshing stateful filtered lists
public class AvailableDatasetsFilter implements Filter<DBDataset> {
    private String nameToken;
    private Set<DBObjectType> objectTypes = new HashSet<>();
    private Set<String> selectedElements = new HashSet<>();

    public AvailableDatasetsFilter() {
        objectTypes.add(DBObjectType.TABLE);
    }

    @Override
    public boolean accepts(DBDataset dataset) {
        return
            isObjectTypeMatch(dataset) &&
            isObjectNameMatch(dataset) &&
            isSelectionMatch(dataset);
    }

    private boolean isObjectTypeMatch(DBDataset object) {
        return objectTypes.contains(object.getObjectType());
    }

    public boolean isObjectNameMatch(DBDataset object) {
        return nameToken == null || Strings.containsIgnoreCase(object.getName(), nameToken);
    }

    public boolean isSelectionMatch(DBDataset object) {
        return !selectedElements.contains(object.getQualifiedName());
    }

}
