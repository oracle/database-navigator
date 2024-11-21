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
import com.intellij.formatting.Indent;
import com.intellij.formatting.Spacing;
import com.intellij.formatting.Wrap;
import lombok.Getter;

@Getter
public class FormattingAttributes {
    public static final FormattingAttributes NO_ATTRIBUTES = new FormattingAttributes(null, null, null, null);

    public enum Type {
        WRAP(true),
        INDENT(true),
        SPACING_BEFORE(true),
        SPACING_AFTER(false);

        boolean left;
        private Type(boolean left) {
            this.left = left;
        }
        public boolean isLeft() {
            return left;
        }
    }



    private WrapDefinition wrap;
    private IndentDefinition indent;
    private SpacingDefinition spacingBefore;
    private SpacingDefinition spacingAfter;

    public FormattingAttributes(WrapDefinition wrap, IndentDefinition indent, SpacingDefinition spacingBefore, SpacingDefinition spacingAfter) {
        this.wrap = wrap;
        this.indent = indent;
        this.spacingBefore = spacingBefore;
        this.spacingAfter = spacingAfter;
    }

    public FormattingAttributes(FormattingAttributes source) {
        this.wrap = source.wrap;
        this.indent = source.indent;
        this.spacingBefore = source.spacingBefore;
        this.spacingAfter = source.spacingAfter;
    }

    public static FormattingAttributes copy(FormattingAttributes source) {
        return source == null ? null : new FormattingAttributes(source);
    }

    public static FormattingAttributes merge(FormattingAttributes attributes, FormattingAttributes defaultAttributes) {
        if (defaultAttributes != null) {
            if (attributes == null) {
                attributes = new FormattingAttributes(defaultAttributes);
            } else {
                attributes.wrap = Commons.nvln(attributes.wrap, defaultAttributes.wrap);
                attributes.indent = Commons.nvln(attributes.indent, defaultAttributes.indent);
                attributes.spacingBefore = Commons.nvln(attributes.spacingBefore, defaultAttributes.spacingBefore);
                attributes.spacingAfter = Commons.nvln(attributes.spacingAfter, defaultAttributes.spacingAfter);
            }
        }
        return attributes;
    }

    public static FormattingAttributes overwrite(FormattingAttributes attributes, FormattingAttributes defaultAttributes) {
        if (attributes != null && defaultAttributes != null) {
            attributes.wrap = Commons.nvln(defaultAttributes.wrap, attributes.wrap);
            attributes.indent = Commons.nvln(defaultAttributes.indent, attributes.indent);
            attributes.spacingBefore = Commons.nvln(defaultAttributes.spacingBefore, attributes.spacingBefore);
            attributes.spacingAfter = Commons.nvln(defaultAttributes.spacingAfter, attributes.spacingAfter);
        }
        return attributes;
    }

    public Wrap getWrap() {
        return wrap == null ? null : wrap.getValue();
    }

    public Indent getIndent() {
        return indent == null ? null : indent.getValue();
    }

    public Spacing getSpacingBefore() {
        return spacingBefore == null ? null : spacingBefore.getValue();
    }

    public Spacing getSpacingAfter() {
        return spacingAfter == null ? null : spacingAfter.getValue();
    }

    public boolean isEmpty() {
        return wrap == null && indent == null && spacingBefore == null && spacingAfter == null;
    }

    public Object getAttribute(Type type) {
        switch (type) {
            case WRAP: return wrap;
            case INDENT: return indent;
            case SPACING_BEFORE: return spacingBefore;
            case SPACING_AFTER: return spacingAfter;
        }
        return null;
    }

    public static Object getAttribute(FormattingAttributes attributes, Type type) {
        return attributes == null ? null : attributes.getAttribute(type);
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("");
        if (wrap != null) result.append(" wrap=").append(wrap);
        if (indent != null) result.append(" indent=").append(indent);
        if (spacingBefore != null) result.append(" spacingBefore=").append(spacingBefore);
        if (spacingAfter != null) result.append(" spacingAfter=").append(spacingAfter);

        return result.toString();
    }
}
