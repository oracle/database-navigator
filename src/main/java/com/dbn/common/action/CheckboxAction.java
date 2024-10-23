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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.project.DumbAware;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public abstract class CheckboxAction extends com.intellij.openapi.actionSystem.ex.CheckboxAction implements BackgroundUpdateAware, DumbAware {
    protected CheckboxAction() {
    }

    protected CheckboxAction(String text) {
        super(text);
    }

    protected CheckboxAction(String text, String description, Icon icon) {
        super(text, description, icon);
    }

    @NotNull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return resolveActionUpdateThread();
    }

}
