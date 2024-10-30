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

import lombok.experimental.UtilityClass;

/**
 * Utility class allowing creation of different types of {@link Outcome}
 *
 * @author Dan Cioca (Oracle)
 */
@UtilityClass
public class Outcomes {
    public static Outcome success(String title, String message) {
        return new Outcome(OutcomeType.SUCCESS, title, message);
    }

    public static Outcome warning(String title, String message) {
        return new Outcome(OutcomeType.WARNING, title, message);
    }

    public static Outcome warning(String title, String message, Exception exception) {
        return new Outcome(OutcomeType.WARNING, title, message, exception);
    }

    public static Outcome failure(String title, String message, Exception exception) {
        return new Outcome(OutcomeType.FAILURE, title, message, exception);
    }
}
