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

package com.dbn.connection.config;

import com.dbn.common.clipboard.Clipboard;
import com.dbn.common.util.XmlContents;
import com.dbn.connection.ConnectionResourceLimits;
import lombok.experimental.UtilityClass;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static com.dbn.common.options.setting.Settings.newElement;
import static com.dbn.connection.ConnectionResourceLimits.isClipboardImportAllowed;

@UtilityClass
public class ConnectionConfigExport {
    private static final @NonNls String CONNECTION_CONFIGURATIONS_ELEMENT = "connection-configurations";
    private static final @NonNls String DBN_EXPORT_ATTRIBUTE = "dbn-export";
    private static final @NonNls String DBN_EXPORT_VERSION_ATTRIBUTE = "dbn-export-version";
    private static final @NonNls String DBN_EXPORT_CONNECTIONS = "connection-configurations";
    private static final @NonNls String DBN_EXPORT_VERSION = "1";

    public static Element createConnectionConfigElement() {
        Element rootElement = newElement(CONNECTION_CONFIGURATIONS_ELEMENT);
        rootElement.setAttribute(DBN_EXPORT_ATTRIBUTE, DBN_EXPORT_CONNECTIONS);
        rootElement.setAttribute(DBN_EXPORT_VERSION_ATTRIBUTE, DBN_EXPORT_VERSION);
        return rootElement;
    }

    public static boolean isConnectionConfig() {
        return readClipboardElement() != null;
    }

    @Nullable
    public static Element readClipboardElement() {
        Element rootElement = loadClipboardElement();
        return isConnectionConfig(rootElement) ? rootElement : null;
    }

    @Nullable
    private static Element loadClipboardElement() {
        String clipboardData = Clipboard.getStringContent();
        if (clipboardData == null) return null;
        if (!isClipboardImportAllowed(clipboardData)) return null;

        byte[] clipboardBytes = clipboardData.getBytes(StandardCharsets.UTF_8);

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(clipboardBytes)) {
            return XmlContents.streamToElement(inputStream);
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean isConnectionConfig(@Nullable Element rootElement) {
        return rootElement != null &&
                CONNECTION_CONFIGURATIONS_ELEMENT.equals(rootElement.getName()) &&
                DBN_EXPORT_CONNECTIONS.equals(rootElement.getAttributeValue(DBN_EXPORT_ATTRIBUTE)) &&
                DBN_EXPORT_VERSION.equals(rootElement.getAttributeValue(DBN_EXPORT_VERSION_ATTRIBUTE));
    }
}
