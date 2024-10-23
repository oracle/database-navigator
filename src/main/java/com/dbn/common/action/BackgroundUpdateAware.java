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

package com.dbn.common.action;

import com.dbn.common.Reflection;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.ActionUpdateThreadAware;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

/**
 * ActionUpdateThread decision stub, resolving teh {@link ActionUpdateThread} based on the {@link BackgroundUpdate} annotation
 * Actions that take long to update their presentation should be annotated accordingly
 * (i.e. actions that have intensive computational logic inside the {@link com.intellij.openapi.actionSystem.AnAction#update(AnActionEvent)})
 *
 * @author Dan Cioca (Oracle)
 */
public interface BackgroundUpdateAware extends ActionUpdateThreadAware {

    /**
     * Use this to implement getActionUpdateThread() overrides in all Action abstract stubs
     * @return an {@link ActionUpdateThread} identifier
     */
    @NotNull
    default ActionUpdateThread resolveActionUpdateThread() {
        boolean updateInBackground = Reflection.hasAnnotation(getClass(), BackgroundUpdate.class);
        return updateInBackground ? ActionUpdateThread.BGT : ActionUpdateThread.EDT;
    }
}
