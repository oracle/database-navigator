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

package com.dbn.code.common.style.formatting;

import com.dbn.common.util.Commons;
import org.jdom.Element;

public class FormattingDefinition {
    public static final FormattingDefinition LINE_BREAK_BEFORE = new FormattingDefinition(null, null, SpacingDefinition.MIN_LINE_BREAK, null);
    public static final FormattingDefinition LINE_BREAK_AFTER = new FormattingDefinition(null, null, null, SpacingDefinition.MIN_LINE_BREAK);

    public static final FormattingDefinition ONE_SPACE_BEFORE = new FormattingDefinition(null, null, SpacingDefinition.ONE_SPACE, null);
    public static final FormattingDefinition NO_SPACE_BEFORE = new FormattingDefinition(null, null, SpacingDefinition.NO_SPACE, null);

    private WrapDefinition wrap;
    private IndentDefinition indent;
    private SpacingDefinition spacingBefore;
    private SpacingDefinition spacingAfter;
    private FormattingAttributes attributes;

    public FormattingDefinition(){
    }

    public FormattingDefinition(WrapDefinition wrap, IndentDefinition indent, SpacingDefinition spacingBefore, SpacingDefinition spacingAfter) {
        this.wrap = wrap;
        this.indent = indent;
        this.spacingBefore = spacingBefore;
        this.spacingAfter = spacingAfter;
    }

    protected FormattingDefinition(FormattingDefinition attributes) {
        indent = attributes.indent;
        wrap = attributes.wrap;
        spacingBefore = attributes.spacingBefore;
        spacingAfter = attributes.spacingAfter;
    }

    protected FormattingDefinition(Element def) {
        indent = IndentDefinition.get(def);
        wrap = WrapDefinition.get(def);
        spacingBefore = SpacingDefinition.get(def, true);
        spacingAfter = SpacingDefinition.get(def, false);
    }

    public void merge(FormattingDefinition defaults) {
        wrap = Commons.nvln(wrap, defaults.wrap);
        indent = Commons.nvln(indent, defaults.indent);
        spacingBefore = Commons.nvln(spacingBefore, defaults.spacingBefore);
        spacingAfter = Commons.nvln(spacingAfter, defaults.spacingAfter);
    }
    
    public FormattingAttributes getAttributes() {
        if (attributes == null) {
            attributes = new FormattingAttributes(wrap, indent, spacingBefore, spacingAfter);
        }
        return attributes;
    }

    public boolean isEmpty() {
        return  wrap == null &&
                indent == null &&
                spacingBefore == null &&
                spacingAfter == null;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("");
        if (wrap != null) result.append("wrap=").append(wrap).append(' ');
        if (indent != null) result.append("indent=").append(indent).append(' ');
        if (spacingBefore != null) result.append("spacingBefore=").append(spacingBefore).append(' ');
        if (spacingAfter != null) result.append("spacingAfter=").append(spacingAfter).append(' ');

        return result.toString();
    }
}

