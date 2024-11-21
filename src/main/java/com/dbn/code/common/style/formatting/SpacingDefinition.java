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

import com.dbn.code.common.style.presets.CodeStylePreset;
import com.intellij.formatting.Spacing;
import org.jdom.Element;

import java.util.function.Supplier;

import static com.dbn.common.options.setting.Settings.enumAttribute;

public enum SpacingDefinition implements FormattingAttribute<Spacing>{
    NO_SPACE       (() -> CodeStylePreset.SPACING_NO_SPACE),
    ONE_SPACE      (() -> CodeStylePreset.SPACING_ONE_SPACE),
    LINE_BREAK     (() -> CodeStylePreset.SPACING_LINE_BREAK),
    ONE_LINE       (() -> CodeStylePreset.SPACING_ONE_LINE),
    MIN_LINE_BREAK (() -> CodeStylePreset.SPACING_MIN_LINE_BREAK),
    MIN_ONE_LINE   (() -> CodeStylePreset.SPACING_MIN_ONE_LINE),
    MIN_ONE_SPACE  (() -> CodeStylePreset.SPACING_MIN_ONE_SPACE),
    ;

    private Spacing value;
    private Supplier<Spacing> loader;

    private SpacingDefinition(Supplier<Spacing> loader) {
        this.loader = loader;
    }

    @Override
    public Spacing getValue() {
        if (value == null && loader != null) {
            value = loader.get();
            loader = null;
        }
        return value;
    }

    public static SpacingDefinition get(Element element, boolean before) {
        return before ?
                enumAttribute(element, "formatting-spacing-before", SpacingDefinition.class) :
                enumAttribute(element, "formatting-spacing-after", SpacingDefinition.class);
    }
}
