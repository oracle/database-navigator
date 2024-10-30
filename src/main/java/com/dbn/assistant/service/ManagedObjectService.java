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

package com.dbn.assistant.service;

import com.dbn.assistant.entity.AttributeInput;
import com.dbn.connection.ConnectionId;
import com.dbn.object.type.DBObjectType;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Skeleton for a CRUD service
 * @author Emmanuel Jannetti (Oracle)
 * @param <E>
 */
public interface ManagedObjectService<E extends AttributeInput> {
    CompletableFuture<List<E>> list();
    CompletableFuture<E>       get(String uuid);
    CompletableFuture<Void>    delete(String uuid);
    CompletionStage<Void>      create(E newItem);
    CompletionStage<Void>      update(E updatedItem);
    void reset();

    ConnectionId getConnectionId();
    DBObjectType getObjectType();
}
