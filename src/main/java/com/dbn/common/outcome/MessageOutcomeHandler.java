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

package com.dbn.common.outcome;

import com.dbn.common.Priority;
import com.dbn.common.action.UserDataKeys;
import com.dbn.common.util.UserDataHolders;
import com.intellij.openapi.project.Project;

import static com.dbn.common.util.Messages.*;

/**
 * Generic implementation of an {@link OutcomeHandler} that produces message dialogs for the handled outcomes
 * (invokes utilities from {@link com.dbn.common.util.Messages} in accordance with the {@link OutcomeType} of the handled {@link Outcome})
 *
 * @author Dan Cioca (Oracle)
 */
public final class MessageOutcomeHandler extends ProjectOutcomeHandler {
    private MessageOutcomeHandler(Project project) {
        super(project);
    }

    @Override
    public Priority getPriority() {
        return Priority.MEDIUM;
    }

    public static OutcomeHandler get(Project project) {
        return UserDataHolders.ensure(project, UserDataKeys.MESSAGE_OUTCOME_HANDLER, () -> new MessageOutcomeHandler(project));
    }

    @Override
    public void handle(Outcome outcome) {
        Project project = getProject();

        String title = outcome.getTitle();
        String message = outcome.getMessage();
        Exception exception = outcome.getException();

        switch (outcome.getType()) {
            case SUCCESS: showInfoDialog(project, title, message); break;
            case WARNING: showWarningDialog(project, title, message); break;
            case FAILURE: showErrorDialog(project, title, message, exception); break;
        }

    }
}
