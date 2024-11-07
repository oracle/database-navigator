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

package com.dbn.object.management.adapter;

import com.dbn.object.DBCredential;
import com.dbn.object.event.ObjectChangeAction;
import com.dbn.object.management.ObjectManagementAdapter;
import com.dbn.object.management.ObjectManagementAdapterFactory;
import com.dbn.object.management.adapter.credential.DBCredentialCreationAdapter;
import com.dbn.object.management.adapter.credential.DBCredentialDeleteAdapter;
import com.dbn.object.management.adapter.credential.DBCredentialDisableAdapter;
import com.dbn.object.management.adapter.credential.DBCredentialEnableAdapter;
import com.dbn.object.management.adapter.credential.DBCredentialUpdateAdapter;
import org.jetbrains.annotations.Nullable;

/**
 * Implementation of {@link ObjectManagementAdapterFactory} for objects of type {@link DBCredential}
 * @author Dan Cioca (Oracle)
 */
public class DBCredentialManagementAdapter implements ObjectManagementAdapterFactory<DBCredential> {

  @Override
  @Nullable
  public ObjectManagementAdapter<DBCredential> createAdapter(DBCredential credential, ObjectChangeAction action) {
    switch (action) {
      case CREATE: return new DBCredentialCreationAdapter(credential);
      case UPDATE: return new DBCredentialUpdateAdapter(credential);
      case DELETE: return new DBCredentialDeleteAdapter(credential);
      case ENABLE: return new DBCredentialEnableAdapter(credential);
      case DISABLE: return new DBCredentialDisableAdapter(credential);
      default: return null;
    }
  }
}
