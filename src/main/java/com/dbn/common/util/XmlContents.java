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

package com.dbn.common.util;

import com.intellij.openapi.util.JDOMUtil;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.JDOMParseException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.NotNull;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

@Slf4j
@UtilityClass
public final class XmlContents {

    public static Element fileToElement(Class clazz, @NonNls String fileName) throws Exception {
        URL url = clazz.getResource(fileName);
        return streamToDocument(url.openStream(), url).getRootElement();
    }

    public static Element streamToElement(InputStream inputStream) throws Exception{
        return JDOMUtil.load(inputStream);
    }

    /**
     * Loads an XML document from a classpath resource.
     *
     * @return the parsed document, or {@code null} when the resource does not exist
     */
    @Nullable
    public static Document fileToDocument(Class clazz, @NonNls String fileName) throws Exception {
        URL url = clazz.getResource(fileName);
        if (url == null) return null;

        return streamToDocument(url.openStream(), url);
    }

    public static Document streamToDocument(InputStream inputStream) throws Exception{
        SAXBuilder builder = createBuilder();
        return builder.build(inputStream);
    }

    private static @NotNull SAXBuilder createBuilder() {
        SAXBuilder builder = new SAXBuilder();
/*
        builder.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true);
*/
        return builder;
    }

    private static Document streamToDocument(InputStream inputStream, URL url) throws Exception{
        try (inputStream) {
            byte[] bytes = inputStream.readAllBytes();
            try {
                return createBuilder().build(new ByteArrayInputStream(bytes), url.toExternalForm());
            } catch (JDOMParseException e) {
                log.warn("Failed to parse document from {}", url, e);

                if (!isDocTypeDisallowed(e)) throw e;
                return createBuilder().build(new ByteArrayInputStream(stripDocType(bytes)), url.toExternalForm());
            }
        }
    }

    private static boolean isDocTypeDisallowed(JDOMParseException exception) {
        Throwable cause = exception.getCause();
        String message = cause instanceof SAXParseException ? cause.getMessage() : null;
        return message != null && message.contains("disallow-doctype-decl");
    }

    private static byte[] stripDocType(byte[] bytes) {
        int start = indexOfDocType(bytes);
        if (start == -1) return bytes;

        int end = findDocTypeEnd(bytes, start);
        if (end == -1) return bytes;

        byte[] strippedBytes = new byte[bytes.length - (end - start + 1)];
        System.arraycopy(bytes, 0, strippedBytes, 0, start);
        System.arraycopy(bytes, end + 1, strippedBytes, start, bytes.length - end - 1);
        return strippedBytes;
    }

    private static int indexOfDocType(byte[] bytes) {
        byte[] docType = "<!DOCTYPE".getBytes(StandardCharsets.US_ASCII);
        for (int i = 0; i <= bytes.length - docType.length; i++) {
            boolean match = true;
            for (int j = 0; j < docType.length; j++) {
                if (bytes[i + j] != docType[j]) {
                    match = false;
                    break;
                }
            }
            if (match) return i;
        }
        return -1;
    }

    private static int findDocTypeEnd(byte[] bytes, int start) {
        int subsetDepth = 0;
        byte quote = 0;
        boolean comment = false;

        for (int i = start + "<!DOCTYPE".length(); i < bytes.length; i++) {
            if (comment) {
                if (startsWith(bytes, i, "-->")) {
                    comment = false;
                    i += 2;
                }
                continue;
            }

            byte current = bytes[i];
            if (quote != 0) {
                if (current == quote) quote = 0;
                continue;
            }
            if (startsWith(bytes, i, "<!--")) {
                comment = true;
                i += 3;
            } else if (current == '\'' || current == '"') {
                quote = current;
            } else if (current == '[') {
                subsetDepth++;
            } else if (current == ']' && subsetDepth > 0) {
                subsetDepth--;
            } else if (current == '>' && subsetDepth == 0) {
                return i;
            }
        }
        return -1;
    }

    private static boolean startsWith(byte[] bytes, int offset, String value) {
        if (offset + value.length() > bytes.length) return false;
        for (int i = 0; i < value.length(); i++) {
            if (bytes[offset + i] != value.charAt(i)) return false;
        }
        return true;
    }

}
