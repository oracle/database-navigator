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

import com.dbn.common.outcome.OutcomeHandler;
import com.dbn.common.outcome.OutcomeType;
import com.dbn.common.util.Named;
import com.dbn.nls.NlsSupport;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.type.DBObjectType;

/**
 * Wrapper component for database interface calls revolving around a database entity
 * Exposes different ways of invoking the database interface:
 *  - synchronous: in the caller thread
 *  - modal: prompts a modal progress dialog that can be cancelled
 *  - prompted: prompts a modal progress dialog that can be cancelled or sent to background
 *  - background: starts the process in background showing in the IDE "Background Tasks"
 *
 * Allows adding an indefinite number of outcome handlers to be invoked when the process exits.
 * Depending on the {@link OutcomeType} returned by the database interaction, the appropriate handlers will be invoked in their order of priority
 *
 * @param <T> the type of the entity in question
 */
public interface ObjectManagementAdapter<T extends Named> extends NlsSupport {

    /**
     * Returns the action performed by this adapter against the entity
     * (e.g. CREATE, UPDATE, DELETE aso...)
     *
     * @return an {@link ObjectChangeAction}
     */
    ObjectChangeAction getAction();

    /**
     * Returns the type of the object this adapter is capable of handling
     * @return a {@link DBObjectType}
     */
    DBObjectType getObjectType();

    /**
     * Adds a handler for a given process termination outcome
     * @param outcomeType the {@link OutcomeType} the handler is acting against
     * @param handler the {@link OutcomeHandler} to perform on the given outcomeType
     */
    void addOutcomeHandler(OutcomeType outcomeType, OutcomeHandler handler);

    /**
     * Invokes the object management action in the current thread
     * (the caller has to make sure this is not invoked in the dispatch thread)
     */
    void invoke();

    /**
     * Invokes the object management action in a cancelable MODAL process thread
     */
    void invokeModal();

    /**
     * Invokes the object management action in a cancelable PROMPTED process thread
     */
    void invokePrompted();

    /**
     * Invokes the object management action in a cancelable BACKGROUND process thread
     */
    void invokeInBackground();
}
