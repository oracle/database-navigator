/*
 * Copyright 2026 Oracle and/or its affiliates
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

package com.dbn.assistant.credential.ui;

import com.dbn.assistant.credential.AssistantCredential;
import com.dbn.assistant.credential.AssistantCredentialBundle;
import org.junit.Test;

import static com.dbn.common.ui.util.PasswordFields.getPasswordPlaceholder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class AssistantCredentialsTableModelTest {
    @Test
    public void presentsSecretAsPasswordPlaceholder() {
        String secret = "assistant-secret-token";
        AssistantCredential credential = new AssistantCredential();
        credential.setSecret(secret.toCharArray());

        AssistantCredentialBundle credentials = new AssistantCredentialBundle(null);
        credentials.addCredential(credential);

        AssistantCredentialsTableModel model = new AssistantCredentialsTableModel(credentials);
        String presentableValue = model.getPresentableValue(
                model.getValueAt(0, AssistantCredentialsTableCellRenderer.SECRET_COLUMN),
                AssistantCredentialsTableCellRenderer.SECRET_COLUMN);

        assertEquals(getPasswordPlaceholder(secret.length()), presentableValue);
        assertFalse(presentableValue.contains(secret));
    }
}
