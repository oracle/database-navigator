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

package com.dbn.common.util;

import com.dbn.common.Priority;

/**
 * Marker interface for objects that can be prioritized in a given form
 * (e.g. tasks to be executed in a given order of priority)
 * The interface extends and implements an inverted {@link Comparable}, ensuring higher priorities are favored over lower
 *
 *
 * @author Dan Cioca (Oracle)
 */
public interface Prioritized extends Comparable<Prioritized> {
    Priority getPriority();

    @Override
    default int compareTo(Prioritized that) {
        return that.getPriority().compareTo(this.getPriority());
    }
}
