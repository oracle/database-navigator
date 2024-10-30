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
import com.dbn.common.event.ProjectEvents;
import com.dbn.connection.ConnectionHandler;
import com.dbn.connection.ConnectionId;
import com.dbn.object.event.ObjectChangeListener;
import com.intellij.openapi.project.Project;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static java.util.Collections.unmodifiableList;
import static java.util.concurrent.CompletableFuture.completedFuture;

/**
 * Proxy on remote service.
 * The purpose of this proxy si to cache information.
 * As for now there is no auto-invalidation (time based etc...)
 * This proxy fetch remotely a soon as a destruptive operation has been
 * performed
 *
 * @author Emmanuel Jannetti (Oracle)
 * @param <E>
 */
public class ManagedObjectServiceProxy<E extends AttributeInput> extends ManagedObjectServiceDelegate<E> {
  private List<E> items = null;
  private boolean dirty;

  public ManagedObjectServiceProxy(ManagedObjectService<E> backend) {
    super(backend);
  }

  /**
   * make all items invalid
   */
  public void invalidate() {

  }

  @Override
  public CompletableFuture<List<E>> list() {
      if (isValid()) return completedFuture(unmodifiableList(this.items));

      CompletableFuture<List<E>> future = delegate.list();

      // internally capture the result upon completion
      future.thenCompose(list -> {
        this.items = new ArrayList<>(list);
        this.dirty = false;
        return null;
      });

      // pass on to caller to consume the loaded list
      return future;
  }

  private boolean isValid() {
    return this.items != null && !dirty;
  }

  @Override
  public CompletableFuture<E> get(String uuid) {
    Optional<E> item = this.items.stream().filter(e -> e.getUuid().equalsIgnoreCase(uuid)).findFirst();
    if (item.isPresent()) {
      return completedFuture(item.get());
    } else {
      return completedFuture(null);
    }
  }

  @Override
  public CompletableFuture<Void> delete(String uuid) {
    return delegate.delete(uuid).thenRunAsync(() -> {
      this.items.removeIf(e -> e.getUuid().equalsIgnoreCase(uuid));
      dirty = true;
      notifyChanges();
    });
  }

  @Override
  public CompletionStage<Void> create(E newItem) {
    return delegate.create(newItem).thenRunAsync(() -> {
      this.items.add(newItem);
      dirty = true;
      notifyChanges();
    });
  }

  @Override
  public CompletionStage<Void> update(E updatedItem) {
    return delegate.update(updatedItem).thenRunAsync(() -> {
      dirty = true;
      notifyChanges();
    });
  }

  /**
   * Resets the internal state of the service
   * (clears cache layer)
   */
  public void reset(){
    dirty = true;
    delegate.reset();
  }

  private void notifyChanges() {
    ConnectionId connectionId = getConnectionId();
    if (connectionId == null) return;

    ConnectionHandler connection = ConnectionHandler.ensure(connectionId);
    Project project = connection.getProject();
    ProjectEvents.notify(project, ObjectChangeListener.TOPIC,
            l -> l.objectsChanged(connectionId, null, getObjectType()));
  }
}
