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

import com.intellij.openapi.ide.CopyPasteManager;
import org.jetbrains.annotations.Nullable;

import java.awt.datatransfer.DataFlavor;

import static com.dbn.diagnostics.Diagnostics.conditionallyLog;

public class Clipboard {

    public static XmlContent createXmlContent(String text) {
        return new XmlContent(text);
    }

    public static HtmlContent createHtmlContent(String text) {
        return new HtmlContent(text);
    }

    @Nullable
    public static String getStringContent() {
        try {
            CopyPasteManager copyPasteManager = CopyPasteManager.getInstance();
            Object data = copyPasteManager.getContents(DataFlavor.stringFlavor);;
            if (data instanceof String) {
                return (String) data;
            }
        } catch (Throwable e) {
            conditionallyLog(e);
        }

        return null;
    }

}
