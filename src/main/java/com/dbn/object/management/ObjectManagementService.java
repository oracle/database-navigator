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

package com.dbn.object.management;

import com.dbn.common.component.ProjectComponent;
import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.object.common.DBObject;
import com.dbn.object.event.ObjectChangeAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import static com.dbn.common.component.Components.projectService;

public interface ObjectManagementService extends ProjectComponent {

    static ObjectManagementService getInstance(@NotNull Project project) {
        return projectService(project, ObjectManagementService.class);
    }

    boolean supports(DBObject object);

    void createObject(DBObject object, OutcomeHandler successHandler);

    void updateObject(DBObject object, OutcomeHandler successHandler);

    void deleteObject(DBObject object, OutcomeHandler successHandler);

    void enableObject(DBObject object, OutcomeHandler successHandler);

    void disableObject(DBObject object, OutcomeHandler successHandler);

    void changeObject(DBObject object, ObjectChangeAction action, OutcomeHandler successHandler);
}
