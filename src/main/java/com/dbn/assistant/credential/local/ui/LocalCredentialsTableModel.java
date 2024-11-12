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

package com.dbn.assistant.credential.local.ui;

import com.dbn.assistant.credential.local.LocalCredential;
import com.dbn.assistant.credential.local.LocalCredentialBundle;
import com.dbn.common.ui.table.DBNTypedEditableTableModel;
import com.dbn.common.util.Strings;
import com.intellij.openapi.options.ConfigurationException;
import lombok.Getter;

@Getter
public class LocalCredentialsTableModel extends DBNTypedEditableTableModel<LocalCredential> {

  LocalCredentialsTableModel(LocalCredentialBundle credentials) {
    super(LocalCredential.class, credentials.getElements());

    addColumn("Credential Name", String.class, c -> c.getName(), (c, v) -> c.setName(v));
    addColumn("User Name",       String.class, c -> c.getUser(), (c, v) -> c.setUser(v));
    addColumn("Secret",          String.class, c -> c.getKey(),  (c, v) -> c.setKey(v));
  }

  public void validate() throws ConfigurationException {
    for (LocalCredential credential : getElements()) {
      if (Strings.isEmpty(credential.getName())) {
        throw new ConfigurationException("Please provide names for all credentials.");
      }
    }
  }
}
