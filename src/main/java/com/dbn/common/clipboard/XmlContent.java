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

package com.dbn.common.clipboard;

import lombok.extern.slf4j.Slf4j;

import java.awt.datatransfer.DataFlavor;
import java.util.Objects;

@Slf4j
public class XmlContent extends ClipboardContent {

    public XmlContent(String text) {
        super(text);
    }

    @Override
    protected DataFlavor[] createDataFlavors() throws Exception {
        DataFlavor[] dataFlavors = new DataFlavor[4];
        dataFlavors[0] = new DataFlavor("text/xml;class=java.lang.String");
        dataFlavors[1] = new DataFlavor("text/rtf;class=java.lang.String");
        dataFlavors[2] = new DataFlavor("text/plain;class=java.lang.String");
        dataFlavors[3] = DataFlavor.stringFlavor;
        return dataFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        String mimeType = flavor.getMimeType();
        return
            Objects.equals(mimeType, "text/xml") ||
            Objects.equals(mimeType, "text/rtf") ||
            Objects.equals(mimeType, "text/plain");
    }
}
