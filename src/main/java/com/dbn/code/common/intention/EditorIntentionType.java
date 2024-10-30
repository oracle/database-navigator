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

package com.dbn.code.common.intention;

import lombok.Getter;

/**
 * Enumeration for all types of editor intentions registered as {@link EditorIntentionActionBase}
 * Defines the priority of the intention
 * NOTE: (do not refactor) changing the sequence of the elements of this enumeration will impact the display order of editor intentions
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
public enum EditorIntentionType {
    // statement
    EXECUTION_RESULT,
    EXECUTE_STATEMENT,
    DEBUG_STATEMENT,
    EXPLAIN_STATEMENT,
    EXECUTE_SCRIPT,

    DEBUG_METHOD,
    EXECUTE_METHOD,

    // assistant
    ASSISTANT_GENERATE,
    ASSISTANT_EXPLAIN,
    ASSISTANT_PROFILE_SELECT,
    ASSISTANT_PROFILE_SETUP,

    // misc
    CONNECT,
    EDITOR_SETTINGS,
    TOGGLE_LOGGING,

    // context
    SELECT_SESSION,
    SELECT_SCHEMA,
    SELECT_CONNECTION,

}
