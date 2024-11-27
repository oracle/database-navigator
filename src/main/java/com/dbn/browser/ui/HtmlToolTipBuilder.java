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

package com.dbn.browser.ui;

public abstract class HtmlToolTipBuilder implements ToolTipProvider {
    private StringBuilder buffer;
    @Override
    public String getToolTip() {
        buffer = new StringBuilder();
        buffer.append("<html>");
        buffer.append("<table><tr><td><table cellpadding=0 cellspacing=0>\n");

        buildToolTip();

        closeOpenRow();
        buffer.append("</table></td></tr></table>");
        buffer.append("</html>");
        return buffer.toString();
    }

    public abstract void buildToolTip();

    public void append(boolean newRow, String text, boolean bold) {
        append(newRow, text, null, null, bold);
    }

    public void append(boolean newRow, String text, String size, String color, boolean bold) {
        if (newRow) createNewRow();
        if (bold) buffer.append("<b>");
        if (color != null || size != null) {
            buffer.append("<font");
            if (color != null) buffer.append(" color='").append(color).append("'");
            if (size != null) buffer.append(" size='").append(size).append("'");
            buffer.append(">");
        }
        buffer.append(text);
        if (color != null || size != null) buffer.append("</font>");
        if (bold) buffer.append("</b>");
    }


    public void createEmptyRow() {
        closeOpenRow();
        buffer.append("<tr><td>&nbsp;</td></tr>\n");
    }

    private void createNewRow() {
        closeOpenRow();
        buffer.append("<tr><td>");
    }

    private void closeOpenRow() {
        if (buffer.charAt(buffer.length()-1)!= '\n') {
            buffer.append("</td></tr>\n");
        }
    }
}
