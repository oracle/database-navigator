/*
 * Copyright 2024 Oracle and/or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dbn.assistant.credential.local.ui;

import com.dbn.assistant.credential.local.LocalCredential;
import com.dbn.assistant.credential.local.LocalCredentialBundle;
import com.dbn.common.ui.table.DBNTypedEditableTableModel;
import com.dbn.common.util.Chars;
import com.dbn.common.util.Strings;
import com.intellij.openapi.options.ConfigurationException;
import lombok.Getter;

@Getter
public class LocalCredentialsTableModel extends DBNTypedEditableTableModel<LocalCredential> {

  LocalCredentialsTableModel(LocalCredentialBundle credentials) {
    super(LocalCredential.class, credentials.getElements());

    addColumn("Credential Name", String.class, c -> c.getName(), (c, v) -> c.setName(v));
    addColumn("User Name",       String.class, c -> c.getUser(), (c, v) -> c.setUser(v));
    addColumn("Secret",          String.class, c -> Chars.toString(c.getKey()),  (c, v) -> c.setKey(Chars.fromString(v)));
  }

  public void validate() throws ConfigurationException {
    for (LocalCredential credential : getElements()) {
      if (Strings.isEmpty(credential.getName())) {
        throw new ConfigurationException("Please provide names for all credentials.");
      }
    }
  }
}
