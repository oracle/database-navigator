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

import com.intellij.formatting.Indent;
import org.jdom.Element;

import java.util.function.Supplier;

import static com.dbn.common.options.setting.Settings.enumAttribute;

public enum IndentDefinition implements FormattingAttribute<Indent> {
    NONE          (() -> Indent.getNoneIndent()),
    NORMAL        (() -> Indent.getNormalIndent(true)),
    CONTINUE      (() -> Indent.getContinuationIndent()),
    ABSOLUTE_NONE (() -> Indent.getAbsoluteNoneIndent());

    private Indent value;
    private Supplier<Indent> loader;

    IndentDefinition(Supplier<Indent> loader) {
        this.loader = loader;
    }

    @Override
    public Indent getValue() {
        if (value == null && loader != null) {
            value = loader.get();
            loader = null;
        }
        return value;
    }

    public static IndentDefinition get(Element element) {
        return enumAttribute(element, "formatting-indent", IndentDefinition.NORMAL);
    }
}
