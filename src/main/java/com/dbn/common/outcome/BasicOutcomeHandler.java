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
import lombok.Getter;

import java.util.function.Consumer;


@Getter
public final class BasicOutcomeHandler implements OutcomeHandler {
    private final Priority priority;
    private final Consumer<Outcome> consumer;

    private BasicOutcomeHandler(Priority priority, Consumer<Outcome> consumer) {
        this.priority = priority;
        this.consumer = consumer;
    }

    @Override
    public void handle(Outcome outcome) {
        consumer.accept(outcome);
    }

    public static OutcomeHandler create(Priority priority, Consumer<Outcome> consumer) {
        return new BasicOutcomeHandler(priority, consumer);
    }
}
