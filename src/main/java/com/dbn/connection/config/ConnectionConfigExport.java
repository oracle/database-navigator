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

import com.dbn.common.util.XmlContents;
import lombok.experimental.UtilityClass;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;

import static com.dbn.common.options.setting.Settings.newElement;

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

    public static boolean isConnectionConfig(@Nullable String clipboardData) {
        if (clipboardData == null) return false;

        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(clipboardData.getBytes())) {
            Element rootElement = XmlContents.streamToElement(inputStream);
            return isConnectionConfig(rootElement);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isConnectionConfig(@Nullable Element rootElement) {
        return rootElement != null &&
                CONNECTION_CONFIGURATIONS_ELEMENT.equals(rootElement.getName()) &&
                DBN_EXPORT_CONNECTIONS.equals(rootElement.getAttributeValue(DBN_EXPORT_ATTRIBUTE)) &&
                DBN_EXPORT_VERSION.equals(rootElement.getAttributeValue(DBN_EXPORT_VERSION_ATTRIBUTE));
    }
}
