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

package com.dbn.common.interceptor;

import com.dbn.common.Priority;
import com.dbn.common.util.Prioritized;

/**
 * Common purpose task interceptor implementation allowing to execute certain actions before and after completion of the task
 * @param <C> the type of the context the interceptor applicable to
 */
public interface Interceptor<C extends InterceptorContext> extends Prioritized {
    boolean supports(C context);

    InterceptorType<?> getType();

    default Priority getPriority() {
        return Priority.MEDIUM;
    }

    default void before(C context) {
    }

    default void after(C context) {
    }
}
