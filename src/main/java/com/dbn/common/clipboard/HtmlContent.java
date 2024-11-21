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

import java.awt.datatransfer.DataFlavor;
import java.util.Objects;

public class HtmlContent extends ClipboardContent {

    public HtmlContent(String htmlText) {
        super(htmlText);
    }

    @Override
    protected DataFlavor[] createDataFlavors() throws Exception {
        DataFlavor[] dataFlavors = new DataFlavor[3];
        dataFlavors[0] = new DataFlavor("text/html;class=java.lang.String");
        dataFlavors[1] = new DataFlavor("text/rtf;class=java.lang.String");
        dataFlavors[2] = new DataFlavor("text/plain;class=java.lang.String");
        return dataFlavors;
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        String mimeType = flavor.getMimeType();
        return
            Objects.equals(mimeType, "text/html") ||
            Objects.equals(mimeType, "text/rtf") ||
            Objects.equals(mimeType, "text/plain");
    }
}
