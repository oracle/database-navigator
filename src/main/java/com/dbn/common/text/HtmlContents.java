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

package com.dbn.common.text;

import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;

/**
 * Utility class providing methods for manipulating and customizing HTML content.
 * This class is intended to be used for specific transformations in HTML strings,
 * such as adjusting font styles and sizes dynamically based on UI settings.
 *
 */
@UtilityClass
public class HtmlContents {

    @NonNls
    public static String initFonts(String html) {
        // quick hack for R3.5.0 accessibility:
        // TODO use velocity template engine instead / proper font family and size placeholders
        int size = UIUtil.getLabelFont().getSize();
        return html
            .replaceAll("\\$\\{REGULAR_FONT_STYLE}", "font-family:Segoe UI,SansSerif,serif; font-size: " + size + "pt")
            .replaceAll("\\$\\{REGULAR_LARGE_FONT_STYLE}", "font-family:Segoe UI,SansSerif,serif; font-size: " + (size + JBUI.scale(4)) + "pt")
            .replaceAll("\\$\\{MONOSPACE_FONT_STYLE}", "font-family: Courier New, Courier, monospace; font-size: " + size + "pt")
            .replaceAll("\\$\\{MONOSPACE_LARGE_FONT_STYLE}", "font-family: Courier New, Courier, monospace; font-size: " + (size + JBUI.scale(2)) + "pt");
    }
}
