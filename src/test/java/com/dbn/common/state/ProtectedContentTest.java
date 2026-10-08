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

package com.dbn.common.state;

import org.jdom.Element;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

public class ProtectedContentTest {
    @Test
    public void returnsNullWhenEncryptedValueCannotBeDecrypted() {
        Element element = new Element("value");
        element.setAttribute("encrypted", "true");
        element.setText("not-an-encrypted-value");

        ProtectedContent content = new ProtectedContent("test.scope");
        content.readState(element);

        assertNull(content.get());
        assertFalse(content.isEmpty());

        Element writtenElement = new Element("value");
        content.writeState(writtenElement);
        assertEquals("true", writtenElement.getAttributeValue("encrypted"));
        assertEquals("not-an-encrypted-value", writtenElement.getText());
    }
}
