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

import com.dbn.object.common.DBObject;
import com.dbn.object.event.ObjectChangeAction;

/**
 * Object management adapter factory
 * Implementations are expected to create {@link ObjectManagementAdapter} instances for all supported {@link ObjectChangeAction} types
 * @param <T> type of the object the adapter factory is responsible for
 *
 * @author Dan Cioca (Oracle)
 */
public interface ObjectManagementAdapterFactory<T extends DBObject> {
    ObjectManagementAdapter<T> createAdapter(T object, ObjectChangeAction action);
}
