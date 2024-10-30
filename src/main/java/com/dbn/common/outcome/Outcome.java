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

import lombok.Getter;

/**
 * Common purpose process outcome information-holder
 * Used in the {@link OutcomeHandler} framework to capture success or failure
 * information about a given process, and allow multiple handlers to act on the outcome
 *
 * @author Dan Cioca (Oracle)
 */
@Getter
public class Outcome {
    private final OutcomeType type;
    private final String title;
    private final String message;
    private final Exception exception;

    public Outcome(OutcomeType type, String title, String message) {
        this(type, title, message, null);
    }

    public Outcome(OutcomeType type, String title, String message, Exception exception) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.exception = exception;
    }
}
