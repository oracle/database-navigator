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

package com.dbn.common.util;

import org.jdom.Document;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;

public class XmlContentsTest {

    @Test
    public void loadsResourceWithDocType() throws Exception {
        Document document = XmlContents.fileToDocument(XmlContentsTest.class, "xml-contents-doctype.xml");

        assertEquals("root", document.getRootElement().getName());
    }

    @Test
    public void loadsDocumentWithNonAsciiSystemId() throws Exception {
        Path directory = Files.createTempDirectory("xml-contents-\u9E5C");
        Path dtd = directory.resolve("xml-contents.dtd");
        Path xml = directory.resolve("xml-contents.xml");

        try {
            Files.writeString(dtd, "<!ELEMENT root EMPTY>\n", StandardCharsets.UTF_8);
            Files.writeString(xml,
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<!DOCTYPE root SYSTEM \"xml-contents.dtd\">\n" +
                            "<root/>\n",
                    StandardCharsets.UTF_8);

            String encodedUrl = xml.toUri().toString();
            String rawUrl = encodedUrl.replace("%E9%B9%9C", "\u9E5C");
            URL url = new URL(rawUrl);

            Document document = XmlContents.streamToDocument(
                    new ByteArrayInputStream(Files.readAllBytes(xml)), url);

            assertEquals("root", document.getRootElement().getName());
        } finally {
            Files.deleteIfExists(xml);
            Files.deleteIfExists(dtd);
            Files.deleteIfExists(directory);
        }
    }
}
