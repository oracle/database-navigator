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

import com.dbn.common.notification.NotificationGroup;
import com.intellij.openapi.project.Project;

/**
 * Collection of {@link OutcomeHandler} elements
 * The handlers are internally sorted by {@link com.dbn.common.Priority} and handled in the
 * priority sequence when the {@link #handle(Outcome)} method is invoked
 *
 * @author Dan Cioca (Oracle)
 */
public interface OutcomeHandlers {

    /**
     * Handler method passing the given outcome to all the registered handles matching the {@link OutcomeType}
     * (handling is invoked for all handlers irrespective if any in the priority chain fails)
     * @param outcome the {@link Outcome} to be handled
     */
    void handle(Outcome outcome);

    /**
     * Adds a generic outcome handler for the given {@link OutcomeType}
     * @param type the type of the outcome the handler is responsible for
     * @param handler the handler
     */
    void addHandler(OutcomeType type, OutcomeHandler handler);

    /**
     * Adds a {@link MessageOutcomeHandler} to the bundle for the given {@link OutcomeType}
     * (the handler prompts a message dialog)
     *
     * @param type the type of outcome to be handled
     * @param project the project in which the outcome was issued
     */
    void addMessageHandler(OutcomeType type, Project project);

    /**
     * Adds a {@link NotificationOutcomeHandler} to the bundle for the given {@link OutcomeType}
     * (the handler creates IDE {@link com.intellij.notification.Notification}) events
     *
     * @param type the type of outcome to be handled
     * @param project the project in which the outcome was issued
     */
    void addNotificationHandler(OutcomeType type, Project project, NotificationGroup group);
}
